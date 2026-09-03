package com.nadoumi.media;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.common.media.MediaUploadCommand;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.domain.MediaAsset;
import com.nadoumi.media.mapper.MediaAssetMapper;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import com.nadoumi.media.policy.MediaCategoryPolicy.CategoryRule;
import com.nadoumi.media.spi.MediaAssets;
import com.nadoumi.media.spi.MediaChecksums;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link MediaStorageService} test double (spec §I.10 — there is no
 * production filesystem impl). {@link #put} persists a real {@code nad_media_asset}
 * row through the injected {@link MediaAssetMapper} so integration tests see rows,
 * while the bytes and a deterministic asset view are held in memory. Delivery URLs
 * are stable {@code https://fake.local/...} strings and the per-method access-class
 * guard-rails mirror {@code CloudinaryMediaStorage} exactly.
 */
public class FakeMediaStorage implements MediaStorageService {

    private static final String PROVIDER = "FAKE";
    private static final String ENV = "test";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUPERSEDED = "SUPERSEDED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String SYSTEM_ACTOR = "system";
    private static final String HOST = "https://fake.local/";

    private final MediaAssetMapper assetMapper;
    private final Map<Long, MediaAsset> rows = new ConcurrentHashMap<>();
    private final Map<Long, byte[]> blobs = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public FakeMediaStorage(MediaAssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public StoredAsset put(MediaUploadCommand cmd) {
        byte[] bytes = readAll(cmd.source());
        MediaCategory category = cmd.category();
        CategoryRule rule = MediaCategoryPolicy.of(category);
        MediaAccessClass accessClass = cmd.accessClass() != null ? cmd.accessClass() : rule.defaultAccessClass();
        String folder = MediaCategoryPolicy.folder(category, ENV);
        String publicId = folder + "/" + UUID.randomUUID();
        String checksum = cmd.checksumSha256() != null ? cmd.checksumSha256() : MediaChecksums.sha256Hex(bytes);

        MediaAsset row = new MediaAsset();
        row.setId(sequence.incrementAndGet());
        row.setProvider(PROVIDER);
        row.setAccessClass(accessClass.name());
        row.setCategory(category.name());
        row.setResourceType(rule.resourceType());
        row.setDeliveryType(accessClass == MediaAccessClass.PUBLIC ? "upload" : "authenticated");
        row.setPublicId(publicId);
        row.setAssetId("fake-" + row.getId());
        row.setCloudVersion(1L);
        row.setSecureUrl(accessClass == MediaAccessClass.PUBLIC ? HOST + "public/" + publicId : null);
        row.setFolder(folder);
        row.setOriginalFilename(cmd.originalFilename());
        row.setContentType(cmd.declaredContentType());
        row.setByteSize((long) bytes.length);
        row.setChecksumSha256(checksum);
        row.setUploadedBy(cmd.uploadedBy());
        row.setOwnerKind(cmd.owner().kind().name());
        row.setOwnerId(cmd.owner().id());
        row.setStatus(STATUS_ACTIVE);
        row.setCreateBy(SYSTEM_ACTOR);
        row.setCreateTime(LocalDateTime.now());

        assetMapper.insert(row);
        rows.put(row.getId(), row);
        blobs.put(row.getId(), bytes);
        return MediaAssets.toStoredAsset(row);
    }

    @Override
    public StoredAsset replace(long assetId, MediaUploadCommand cmd) {
        MediaAsset previous = rows.get(assetId);
        StoredAsset created = put(cmd);
        assetMapper.updateStatus(assetId, STATUS_SUPERSEDED, created.id(), SYSTEM_ACTOR, null);
        if (previous != null) {
            previous.setStatus(STATUS_SUPERSEDED);
            previous.setSupersededBy(created.id());
        }
        return created;
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
        Instant expiresAt = Instant.now().plus(ttl);
        return new SignedUrl(HOST + row.getPublicId() + "?exp=" + expiresAt.getEpochSecond(), expiresAt);
    }

    @Override
    public ProxyStream openStream(long assetId) {
        MediaAsset row = require(assetId);
        if (row.getAccessClassEnum() == MediaAccessClass.PUBLIC) {
            throw new IllegalArgumentException("openStream is only for PROTECTED/SENSITIVE assets; media asset "
                    + assetId + " is PUBLIC");
        }
        byte[] bytes = blobs.getOrDefault(row.getId(), new byte[0]);
        return new ProxyStream(new ByteArrayInputStream(bytes), row.getContentType(), bytes.length,
                row.getOriginalFilename());
    }

    @Override
    public Optional<StoredAsset> find(long assetId) {
        return Optional.ofNullable(rows.get(assetId)).map(MediaAssets::toStoredAsset);
    }

    @Override
    public void softDelete(long assetId, long actorUserId) {
        assetMapper.updateStatus(assetId, STATUS_DELETED, null, SYSTEM_ACTOR, actorUserId);
        MediaAsset row = rows.get(assetId);
        if (row != null) {
            row.setStatus(STATUS_DELETED);
            row.setDeletedAt(LocalDateTime.now());
            row.setDeletedBy(actorUserId);
        }
    }

    private MediaAsset require(long assetId) {
        MediaAsset row = rows.get(assetId);
        if (row == null) {
            throw new IllegalArgumentException("media asset not found: " + assetId);
        }
        return row;
    }

    private static byte[] readAll(InputStream source) {
        try (InputStream in = source) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read upload bytes", e);
        }
    }
}
