package com.nadoumi.media.spi;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-network {@link MediaStorageService} for local development and demos, used
 * when {@code CLOUDINARY_URL} is not set. Bytes are written under
 * {@code <baseDir>/<folder>/<id>` and served by RuoYi's static handler at
 * {@code <baseUrl>/profile/media/...}. PROTECTED / SENSITIVE assets are handed a
 * plain time-boxed URL / an {@code InputStream} — there is no real signing here,
 * which is acceptable only for local dev.
 *
 * <p>Persistence in {@code nad_media_asset} is identical to the Cloudinary path so
 * every downstream read model behaves the same.</p>
 */
public class LocalFilesystemMediaStorage implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFilesystemMediaStorage.class);

    private static final String PROVIDER = "LOCAL";
    private static final String DELIVERY_UPLOAD = "upload";
    private static final String DELIVERY_AUTHENTICATED = "authenticated";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUPERSEDED = "SUPERSEDED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String SYSTEM_ACTOR = "system";
    private static final String URL_PREFIX = "/profile/media/";

    private final MediaAssetMapper assetMapper;
    private final MediaProperties properties;
    private final Path baseDir;
    private final String baseUrl;

    public LocalFilesystemMediaStorage(MediaAssetMapper assetMapper, MediaProperties properties,
            Path baseDir, String baseUrl) {
        this.assetMapper = assetMapper;
        this.properties = properties;
        this.baseDir = baseDir;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        log.warn("CLOUDINARY_URL is not set — media uploads are stored on the local filesystem at {} "
                + "and served from {}{}. Do NOT use this outside local development.", baseDir, this.baseUrl, URL_PREFIX);
    }

    @Override
    public StoredAsset put(MediaUploadCommand cmd) {
        return MediaAssets.toStoredAsset(write(cmd, resolveAccessClass(cmd)));
    }

    @Override
    public StoredAsset replace(long assetId, MediaUploadCommand cmd) {
        MediaAsset existing = require(assetId);
        MediaAccessClass accessClass = cmd.accessClass() != null ? cmd.accessClass() : existing.getAccessClassEnum();
        MediaAsset created = write(cmd, accessClass);
        assetMapper.updateStatus(assetId, STATUS_SUPERSEDED, created.getId(), SYSTEM_ACTOR, null);
        return MediaAssets.toStoredAsset(created);
    }

    @Override
    public String publicUrl(long assetId) {
        MediaAsset row = require(assetId);
        if (row.getAccessClassEnum() != MediaAccessClass.PUBLIC) {
            throw new IllegalArgumentException("publicUrl is only for PUBLIC assets; media asset " + assetId
                    + " is " + row.getAccessClassEnum());
        }
        return row.getSecureUrl();
    }

    @Override
    public SignedUrl signedUrl(long assetId, Duration ttl) {
        MediaAsset row = require(assetId);
        if (row.getAccessClassEnum() != MediaAccessClass.PROTECTED) {
            throw new IllegalArgumentException("signedUrl is only for PROTECTED assets; media asset " + assetId
                    + " is " + row.getAccessClassEnum());
        }
        return new SignedUrl(baseUrl + URL_PREFIX + row.getPublicId(), Instant.now().plus(ttl));
    }

    @Override
    public ProxyStream openStream(long assetId) {
        MediaAsset row = require(assetId);
        MediaAccessClass accessClass = row.getAccessClassEnum();
        if (accessClass != MediaAccessClass.PROTECTED && accessClass != MediaAccessClass.SENSITIVE) {
            throw new IllegalArgumentException("openStream is only for PROTECTED/SENSITIVE assets; media asset "
                    + assetId + " is " + accessClass);
        }
        Path file = baseDir.resolve(row.getPublicId());
        try {
            long length = Files.size(file);
            return new ProxyStream(Files.newInputStream(file), row.getContentType(), length,
                    row.getOriginalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to open local media asset " + assetId + " at " + file, e);
        }
    }

    @Override
    public Optional<StoredAsset> find(long assetId) {
        return Optional.ofNullable(assetMapper.findById(assetId)).map(MediaAssets::toStoredAsset);
    }

    @Override
    public void softDelete(long assetId, long actorUserId) {
        assetMapper.updateStatus(assetId, STATUS_DELETED, null, SYSTEM_ACTOR, actorUserId);
    }

    @Override
    public void purge(long assetId) {
        MediaAsset row = require(assetId);
        Path file = baseDir.resolve(row.getPublicId());
        try {
            boolean removed = Files.deleteIfExists(file);
            log.info("local media asset purged: id={} path={} removed={}", assetId, file, removed);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to delete local media asset " + assetId + " at " + file, e);
        }
    }

    // ---- internals ----

    private MediaAsset write(MediaUploadCommand cmd, MediaAccessClass accessClass) {
        CategoryRule rule = MediaCategoryPolicy.of(cmd.category());
        String folder = MediaCategoryPolicy.folder(cmd.category(), properties.getEnv());
        String extension = extensionFor(cmd.originalFilename());
        String publicId = folder + "/" + UUID.randomUUID() + extension;

        byte[] bytes = readAll(cmd.source());
        Path target = baseDir.resolve(publicId);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write local media asset for category " + cmd.category(), e);
        }

        String deliveryType = accessClass == MediaAccessClass.PUBLIC ? DELIVERY_UPLOAD : DELIVERY_AUTHENTICATED;
        MediaAsset row = new MediaAsset();
        row.setProvider(PROVIDER);
        row.setAccessClass(accessClass.name());
        row.setCategory(cmd.category().name());
        row.setResourceType(rule.resourceType());
        row.setDeliveryType(deliveryType);
        row.setPublicId(publicId);
        row.setSecureUrl(accessClass == MediaAccessClass.PUBLIC ? baseUrl + URL_PREFIX + publicId : null);
        row.setFolder(folder);
        row.setOriginalFilename(cmd.originalFilename());
        row.setContentType(cmd.declaredContentType());
        row.setByteSize((long) bytes.length);
        row.setChecksumSha256(cmd.checksumSha256());
        row.setUploadedBy(cmd.uploadedBy());
        row.setOwnerKind(cmd.owner().kind().name());
        row.setOwnerId(cmd.owner().id());
        row.setStatus(STATUS_ACTIVE);
        row.setCreateBy(SYSTEM_ACTOR);

        assetMapper.insert(row);
        log.info("local media asset stored: id={} path={} bytes={} owner={}:{} access={}",
                row.getId(), publicId, bytes.length, row.getOwnerKind(), row.getOwnerId(), accessClass);
        return row;
    }

    private MediaAccessClass resolveAccessClass(MediaUploadCommand cmd) {
        return cmd.accessClass() != null
                ? cmd.accessClass()
                : MediaCategoryPolicy.of(cmd.category()).defaultAccessClass();
    }

    private MediaAsset require(long assetId) {
        MediaAsset row = assetMapper.findById(assetId);
        if (row == null) {
            throw new IllegalArgumentException("media asset not found: " + assetId);
        }
        return row;
    }

    private static String extensionFor(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 && dot < filename.length() - 1 ? filename.substring(dot).toLowerCase() : "";
    }

    private static byte[] readAll(InputStream source) {
        try (InputStream in = source) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read upload bytes", e);
        }
    }
}
