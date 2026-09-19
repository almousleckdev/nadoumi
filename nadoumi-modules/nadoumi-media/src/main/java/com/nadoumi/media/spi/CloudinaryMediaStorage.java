package com.nadoumi.media.spi;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.media.config.MediaProperties;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAssetMapper;
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
public class CloudinaryMediaStorage extends AbstractMediaStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryMediaStorage.class);

    private static final String PROVIDER = "CLOUDINARY";
    private static final int HTTP_OK = 200;
    private static final long INTERNAL_FETCH_TTL_SECONDS = 60L;
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final long UNKNOWN_LENGTH = -1L;

    private final Cloudinary cloudinary;
    private final HttpClient httpClient;

    public CloudinaryMediaStorage(Cloudinary cloudinary, MediaAssetMapper assetMapper, MediaProperties properties) {
        super(assetMapper, properties);
        if (cloudinary == null
                || !StringUtils.hasText(cloudinary.config.cloudName)
                || !StringUtils.hasText(cloudinary.config.apiKey)
                || !StringUtils.hasText(cloudinary.config.apiSecret)) {
            throw new IllegalStateException("CLOUDINARY_URL is not configured");
        }
        this.cloudinary = cloudinary;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_CONNECT_TIMEOUT).build();
    }

    @Override
    protected String provider() {
        return PROVIDER;
    }

    @Override
    protected StoredObject store(MediaUploadCommand cmd, byte[] bytes, String deliveryType, String folder,
            CategoryRule rule) {
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
        return new StoredObject(
                ObjectUtils.asString(result.get("public_id")),
                ObjectUtils.asString(result.get("resource_type"), rule.resourceType()),
                ObjectUtils.asString(result.get("asset_id"), null),
                ObjectUtils.asLong(result.get("version"), null),
                ObjectUtils.asString(result.get("secure_url"), null),
                ObjectUtils.asLong(result.get("bytes"), (long) bytes.length),
                ObjectUtils.asInteger(result.get("width"), null),
                ObjectUtils.asInteger(result.get("height"), null));
    }

    @Override
    protected SignedUrl signed(MediaAsset row, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        return new SignedUrl(providerDownloadUrl(row, expiresAt), expiresAt);
    }

    @Override
    protected ProxyStream openProviderStream(MediaAsset row) {
        String internalUrl = providerDownloadUrl(row, Instant.now().plusSeconds(INTERNAL_FETCH_TTL_SECONDS));
        HttpResponse<InputStream> response = fetch(internalUrl, row.getId());
        if (response.statusCode() != HTTP_OK) {
            drainQuietly(response, row.getId());
            throw new IllegalStateException("provider returned HTTP " + response.statusCode()
                    + " for media asset " + row.getId());
        }
        String contentType = response.headers().firstValue("content-type").orElse(row.getContentType());
        long contentLength = response.headers().firstValueAsLong("content-length")
                .orElse(row.getByteSize() == null ? UNKNOWN_LENGTH : row.getByteSize());
        log.info("media asset proxied: id={} public_id={} bytes={}", row.getId(), row.getPublicId(), contentLength);
        return new ProxyStream(response.body(), contentType, contentLength, row.getOriginalFilename());
    }

    @Override
    public void purge(long assetId) {
        MediaAsset row = require(assetId);
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().destroy(row.getPublicId(), ObjectUtils.asMap(
                    "resource_type", row.getResourceType(),
                    "type", row.getDeliveryType(),
                    "invalidate", true));
        } catch (IOException e) {
            throw new UncheckedIOException("Cloudinary destroy failed for media asset " + assetId, e);
        }
        log.info("media asset purged from provider: id={} public_id={} result={}",
                assetId, row.getPublicId(), ObjectUtils.asString(result.get("result"), "unknown"));
    }

    /**
     * A signed, time-limited Cloudinary download URL for the original object
     * ({@code /<resource_type>/download?...expires_at=...}). Used verbatim as the
     * PROTECTED signed URL, and internally (never surfaced) by {@link #openProviderStream}.
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
}
