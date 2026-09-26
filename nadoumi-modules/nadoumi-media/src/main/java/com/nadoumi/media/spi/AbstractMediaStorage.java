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
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything the media providers share: the {@code nad_media_asset} row every upload writes, the
 * access-class guards on each delivery method, supersede-on-replace, and soft delete. A provider
 * supplies only how bytes are stored, how a PROTECTED download URL is issued, how bytes are
 * streamed to the backend, and how an object is purged, so a new provider cannot drift on the
 * persistence or the guard rails.
 */
abstract class AbstractMediaStorage implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(AbstractMediaStorage.class);

    protected static final String DELIVERY_UPLOAD = "upload";
    protected static final String DELIVERY_AUTHENTICATED = "authenticated";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUPERSEDED = "SUPERSEDED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String SYSTEM_ACTOR = "system";

    protected final MediaAssetMapper assetMapper;
    protected final MediaProperties properties;

    protected AbstractMediaStorage(MediaAssetMapper assetMapper, MediaProperties properties) {
        this.assetMapper = assetMapper;
        this.properties = properties;
    }

    /** What a provider reports once the bytes are stored. {@code publicUrl} matters only for PUBLIC assets. */
    protected record StoredObject(String publicId, String resourceType, String providerAssetId, Long cloudVersion,
            String publicUrl, long bytes, Integer width, Integer height) {
    }

    /** The provider name recorded on the row, e.g. {@code CLOUDINARY}. */
    protected abstract String provider();

    /** Stores the bytes and describes where they went. */
    protected abstract StoredObject store(MediaUploadCommand cmd, byte[] bytes, String deliveryType, String folder,
            CategoryRule rule);

    /** A short-lived, forced-download URL for a PROTECTED asset. */
    protected abstract SignedUrl signed(MediaAsset row, Duration ttl);

    /** A short-lived URL for a PROTECTED asset meant for inline rendering, never a forced download. */
    protected abstract SignedUrl signedInline(MediaAsset row, Duration ttl);

    /** Opens the bytes of a PROTECTED / SENSITIVE asset for backend proxying. */
    protected abstract ProxyStream openProviderStream(MediaAsset row);

    @Override
    public StoredAsset put(MediaUploadCommand cmd) {
        return MediaAssets.toStoredAsset(persist(cmd, resolveAccessClass(cmd)));
    }

    @Override
    public StoredAsset replace(long assetId, MediaUploadCommand cmd) {
        MediaAsset existing = require(assetId);
        MediaAccessClass accessClass = cmd.accessClass() != null ? cmd.accessClass() : existing.getAccessClassEnum();
        MediaAsset created = persist(cmd, accessClass);
        assetMapper.updateStatus(assetId, STATUS_SUPERSEDED, created.getId(), SYSTEM_ACTOR, null);
        return MediaAssets.toStoredAsset(created);
    }

    @Override
    public String publicUrl(long assetId) {
        MediaAsset row = require(assetId);
        requireAccessClass(row, MediaAccessClass.PUBLIC, "publicUrl");
        return row.getSecureUrl();
    }

    @Override
    public SignedUrl signedUrl(long assetId, Duration ttl) {
        MediaAsset row = require(assetId);
        requireAccessClass(row, MediaAccessClass.PROTECTED, "signedUrl");
        return signed(row, ttl);
    }

    @Override
    public SignedUrl inlineSignedUrl(long assetId, Duration ttl) {
        MediaAsset row = require(assetId);
        requireAccessClass(row, MediaAccessClass.PROTECTED, "inlineSignedUrl");
        return signedInline(row, ttl);
    }

    @Override
    public ProxyStream openStream(long assetId) {
        MediaAsset row = require(assetId);
        MediaAccessClass accessClass = row.getAccessClassEnum();
        if (accessClass != MediaAccessClass.PROTECTED && accessClass != MediaAccessClass.SENSITIVE) {
            throw new IllegalArgumentException("openStream is only for PROTECTED/SENSITIVE assets; media asset "
                    + assetId + " is " + accessClass);
        }
        return openProviderStream(row);
    }

    @Override
    public Optional<StoredAsset> find(long assetId) {
        return Optional.ofNullable(assetMapper.findById(assetId)).map(MediaAssets::toStoredAsset);
    }

    @Override
    public void softDelete(long assetId, long actorUserId) {
        assetMapper.updateStatus(assetId, STATUS_DELETED, null, SYSTEM_ACTOR, actorUserId);
    }

    protected MediaAsset require(long assetId) {
        MediaAsset row = assetMapper.findById(assetId);
        if (row == null) {
            throw new IllegalArgumentException("media asset not found: " + assetId);
        }
        return row;
    }

    private MediaAsset persist(MediaUploadCommand cmd, MediaAccessClass accessClass) {
        CategoryRule rule = MediaCategoryPolicy.of(cmd.category());
        byte[] bytes = readAll(cmd.source());
        String deliveryType = accessClass == MediaAccessClass.PUBLIC ? DELIVERY_UPLOAD : DELIVERY_AUTHENTICATED;
        String folder = MediaCategoryPolicy.folder(cmd.category(), properties.getEnv());
        StoredObject stored = store(cmd, bytes, deliveryType, folder, rule);

        MediaAsset row = new MediaAsset();
        row.setProvider(provider());
        row.setAccessClass(accessClass.name());
        row.setCategory(cmd.category().name());
        row.setResourceType(stored.resourceType());
        row.setDeliveryType(deliveryType);
        row.setPublicId(stored.publicId());
        row.setAssetId(stored.providerAssetId());
        row.setCloudVersion(stored.cloudVersion());
        row.setSecureUrl(accessClass == MediaAccessClass.PUBLIC ? stored.publicUrl() : null);
        row.setFolder(folder);
        row.setOriginalFilename(cmd.originalFilename());
        row.setContentType(cmd.declaredContentType());
        row.setByteSize(stored.bytes());
        row.setWidth(stored.width());
        row.setHeight(stored.height());
        row.setChecksumSha256(cmd.checksumSha256());
        row.setUploadedBy(cmd.uploadedBy());
        row.setOwnerKind(cmd.owner().kind().name());
        row.setOwnerId(cmd.owner().id());
        row.setStatus(STATUS_ACTIVE);
        row.setCreateBy(SYSTEM_ACTOR);

        assetMapper.insert(row);
        // public_id and sizes only: never a delivery URL for a PROTECTED / SENSITIVE asset
        log.info("media asset stored: id={} provider={} public_id={} bytes={} owner={}:{} access={}",
                row.getId(), provider(), row.getPublicId(), row.getByteSize(), row.getOwnerKind(), row.getOwnerId(),
                accessClass);
        return row;
    }

    private MediaAccessClass resolveAccessClass(MediaUploadCommand cmd) {
        return cmd.accessClass() != null
                ? cmd.accessClass()
                : MediaCategoryPolicy.of(cmd.category()).defaultAccessClass();
    }

    private void requireAccessClass(MediaAsset row, MediaAccessClass expected, String operation) {
        if (row.getAccessClassEnum() != expected) {
            throw new IllegalArgumentException(operation + " is only for " + expected + " assets; media asset "
                    + row.getId() + " is " + row.getAccessClassEnum());
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
