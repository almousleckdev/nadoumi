package com.nadoumi.media.spi;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAssetMapper;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import com.nadoumi.media.policy.MediaCategoryPolicy.CategoryRule;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * The sole production {@link MediaStorageService}. Uploads bytes to Cloudinary and
 * keeps the Nadoumi record in {@code nad_media_asset}; delivery is resolved per
 * access class (spec §I.6):
 *
 * <ul>
 *   <li>{@code PUBLIC} — {@code type=upload}; the {@code secure_url} is stored and
 *       returned directly.</li>
 *   <li>{@code PROTECTED} — {@code type=authenticated}; {@link #signedUrl} mints a
 *       short-TTL expiring download URL. No URL is ever stored.</li>
 *   <li>{@code SENSITIVE} — {@code type=authenticated}; {@link #openStream} fetches
 *       the bytes server-side through a 60-second internal URL that is never
 *       logged or surfaced to a client.</li>
 * </ul>
 *
 * <p>Logs {@code public_id} + byte size + owner only — never a delivery URL for a
 * PROTECTED/SENSITIVE asset, never the API secret (spec §I.10).</p>
 */
public class CloudinaryMediaStorage implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryMediaStorage.class);

    private static final String PROVIDER = "CLOUDINARY";
    private static final String DELIVERY_UPLOAD = "upload";
    private static final String DELIVERY_AUTHENTICATED = "authenticated";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUPERSEDED = "SUPERSEDED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String SYSTEM_ACTOR = "system";
    private static final int HTTP_OK = 200;
    private static final long INTERNAL_FETCH_TTL_SECONDS = 60L;
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final long UNKNOWN_LENGTH = -1L;

    private final Cloudinary cloudinary;
    private final MediaAssetMapper assetMapper;
    private final MediaProperties properties;
    private final HttpClient httpClient;

    public CloudinaryMediaStorage(Cloudinary cloudinary, MediaAssetMapper assetMapper, MediaProperties properties) {
        if (cloudinary == null
                || !StringUtils.hasText(cloudinary.config.cloudName)
                || !StringUtils.hasText(cloudinary.config.apiKey)
                || !StringUtils.hasText(cloudinary.config.apiSecret)) {
            throw new IllegalStateException("CLOUDINARY_URL is not configured");
        }
        this.cloudinary = cloudinary;
        this.assetMapper = assetMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_CONNECT_TIMEOUT).build();
    }

    @Override
    public StoredAsset put(MediaUploadCommand cmd) {
        return MediaAssets.toStoredAsset(upload(cmd, resolveAccessClass(cmd)));
    }

    @Override
    public StoredAsset replace(long assetId, MediaUploadCommand cmd) {
        MediaAsset existing = require(assetId);
        MediaAccessClass accessClass = cmd.accessClass() != null
                ? cmd.accessClass()
                : existing.getAccessClassEnum();
        MediaAsset created = upload(cmd, accessClass);
        assetMapper.updateStatus(assetId, STATUS_SUPERSEDED, created.getId(), SYSTEM_ACTOR, null);
        return MediaAssets.toStoredAsset(created);
    }

    @Override
    public String publicUrl(long assetId) {
        MediaAsset row = require(assetId);
        requireAccessClass(row, assetId, MediaAccessClass.PUBLIC, "publicUrl");
        return row.getSecureUrl();
    }

    @Override
    public SignedUrl signedUrl(long assetId, Duration ttl) {
        MediaAsset row = require(assetId);
        requireAccessClass(row, assetId, MediaAccessClass.PROTECTED, "signedUrl");
        Instant expiresAt = Instant.now().plus(ttl);
        return new SignedUrl(providerDownloadUrl(row, expiresAt), expiresAt);
    }

    @Override
    public ProxyStream openStream(long assetId) {
        MediaAsset row = require(assetId);
        MediaAccessClass accessClass = row.getAccessClassEnum();
        if (accessClass != MediaAccessClass.PROTECTED && accessClass != MediaAccessClass.SENSITIVE) {
            throw new IllegalArgumentException("openStream is only for PROTECTED/SENSITIVE assets; media asset "
                    + assetId + " is " + accessClass);
        }
        String internalUrl = providerDownloadUrl(row, Instant.now().plusSeconds(INTERNAL_FETCH_TTL_SECONDS));
        HttpResponse<InputStream> response = fetch(internalUrl, assetId);
        if (response.statusCode() != HTTP_OK) {
            drainQuietly(response, assetId);
            throw new IllegalStateException("provider returned HTTP " + response.statusCode()
                    + " for media asset " + assetId);
        }
        String contentType = response.headers().firstValue("content-type").orElse(row.getContentType());
        long contentLength = response.headers().firstValueAsLong("content-length")
                .orElse(row.getByteSize() == null ? UNKNOWN_LENGTH : row.getByteSize());
        log.info("media asset proxied: id={} public_id={} bytes={}", assetId, row.getPublicId(), contentLength);
        return new ProxyStream(response.body(), contentType, contentLength, row.getOriginalFilename());
    }

    @Override
    public Optional<StoredAsset> find(long assetId) {
        return Optional.ofNullable(assetMapper.findById(assetId)).map(MediaAssets::toStoredAsset);
    }

    @Override
    public void softDelete(long assetId, long actorUserId) {
        assetMapper.updateStatus(assetId, STATUS_DELETED, null, SYSTEM_ACTOR, actorUserId);
    }

    // ---- internals ----

    private MediaAsset upload(MediaUploadCommand cmd, MediaAccessClass accessClass) {
        CategoryRule rule = MediaCategoryPolicy.of(cmd.category());
        byte[] bytes = readAll(cmd.source());
        String deliveryType = accessClass == MediaAccessClass.PUBLIC ? DELIVERY_UPLOAD : DELIVERY_AUTHENTICATED;
        String folder = MediaCategoryPolicy.folder(cmd.category(), properties.getEnv());

        Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "resource_type", rule.resourceType(),
                    "type", deliveryType,
                    "folder", folder,
                    "use_filename", false,
                    "unique_filename", true));
        } catch (IOException e) {
            throw new UncheckedIOException("Cloudinary upload failed for category " + cmd.category(), e);
        }

        MediaAsset row = new MediaAsset();
        row.setProvider(PROVIDER);
        row.setAccessClass(accessClass.name());
        row.setCategory(cmd.category().name());
        row.setResourceType(ObjectUtils.asString(result.get("resource_type"), rule.resourceType()));
        row.setDeliveryType(deliveryType);
        row.setPublicId(ObjectUtils.asString(result.get("public_id")));
        row.setAssetId(ObjectUtils.asString(result.get("asset_id"), null));
        row.setCloudVersion(ObjectUtils.asLong(result.get("version"), null));
        row.setSecureUrl(accessClass == MediaAccessClass.PUBLIC
                ? ObjectUtils.asString(result.get("secure_url"), null)
                : null);
        row.setFolder(folder);
        row.setOriginalFilename(cmd.originalFilename());
        row.setContentType(cmd.declaredContentType());
        row.setByteSize(ObjectUtils.asLong(result.get("bytes"), (long) bytes.length));
        row.setWidth(ObjectUtils.asInteger(result.get("width"), null));
        row.setHeight(ObjectUtils.asInteger(result.get("height"), null));
        row.setChecksumSha256(cmd.checksumSha256());
        row.setUploadedBy(cmd.uploadedBy());
        row.setOwnerKind(cmd.owner().kind().name());
        row.setOwnerId(cmd.owner().id());
        row.setStatus(STATUS_ACTIVE);
        row.setCreateBy(SYSTEM_ACTOR);

        assetMapper.insert(row);
        log.info("media asset stored: id={} public_id={} bytes={} owner={}:{} access={}",
                row.getId(), row.getPublicId(), row.getByteSize(), row.getOwnerKind(), row.getOwnerId(), accessClass);
        return row;
    }

    private MediaAccessClass resolveAccessClass(MediaUploadCommand cmd) {
        return cmd.accessClass() != null
                ? cmd.accessClass()
                : MediaCategoryPolicy.of(cmd.category()).defaultAccessClass();
    }

    /**
     * A signed, time-limited Cloudinary download URL for the original object
     * ({@code /<resource_type>/download?...expires_at=...}). Used verbatim as the
     * PROTECTED signed URL, and internally (never surfaced) by {@link #openStream}.
     */
    private String providerDownloadUrl(MediaAsset row, Instant expiresAt) {
        try {
            return cloudinary.privateDownload(row.getPublicId(), null, ObjectUtils.asMap(
                    "resource_type", row.getResourceType(),
                    "type", DELIVERY_AUTHENTICATED,
                    "expires_at", expiresAt.getEpochSecond()));
        } catch (Exception e) {
            // Cloudinary#privateDownload declares a checked Exception; wrap, never swallow.
            throw new IllegalStateException("failed to sign a delivery URL for media asset " + row.getId(), e);
        }
    }

    private HttpResponse<InputStream> fetch(String url, long assetId) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to fetch media asset " + assetId + " from the provider", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while fetching media asset " + assetId, e);
        }
    }

    private void drainQuietly(HttpResponse<InputStream> response, long assetId) {
        try (InputStream body = response.body()) {
            body.readAllBytes();
        } catch (IOException e) {
            log.debug("failed to drain error-response body for media asset {}", assetId, e);
        }
    }

    private MediaAsset require(long assetId) {
        MediaAsset row = assetMapper.findById(assetId);
        if (row == null) {
            throw new IllegalArgumentException("media asset not found: " + assetId);
        }
        return row;
    }

    private void requireAccessClass(MediaAsset row, long assetId, MediaAccessClass expected, String operation) {
        if (row.getAccessClassEnum() != expected) {
            throw new IllegalArgumentException(operation + " is only for " + expected + " assets; media asset "
                    + assetId + " is " + row.getAccessClassEnum());
        }
    }

    private static byte[] readAll(InputStream source) {
        try (InputStream in = source) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read upload bytes", e);
        }
    }
}
