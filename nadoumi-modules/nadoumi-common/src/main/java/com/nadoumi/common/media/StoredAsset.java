package com.nadoumi.common.media;

import java.time.Instant;

/**
 * Persisted view of one media asset (a {@code nad_media_asset} row). {@code secureUrl}
 * is non-null only for {@link MediaAccessClass#PUBLIC} assets; PROTECTED / SENSITIVE
 * delivery goes through {@link MediaStorageService#signedUrl} / {@code openStream}.
 */
public record StoredAsset(
        long id,
        String provider,
        MediaAccessClass accessClass,
        MediaCategory category,
        String resourceType,
        String deliveryType,
        String publicId,
        String assetId,
        Long cloudVersion,
        String secureUrl,
        String originalFilename,
        String contentType,
        long byteSize,
        Integer width,
        Integer height,
        String checksumSha256,
        long uploadedBy,
        MediaOwnerRef owner,
        String status,
        Instant createdAt) {}
