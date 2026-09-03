package com.nadoumi.media.spi;

import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.domain.MediaAsset;
import java.time.ZoneOffset;

/**
 * Maps the persistence row {@link MediaAsset} to the SPI view {@link StoredAsset}.
 * Shared by {@code CloudinaryMediaStorage} and the test {@code FakeMediaStorage}
 * so both return an identically shaped asset.
 */
public final class MediaAssets {

    private MediaAssets() {
    }

    public static StoredAsset toStoredAsset(MediaAsset row) {
        return new StoredAsset(
                orZero(row.getId()),
                row.getProvider(),
                row.getAccessClassEnum(),
                row.getCategoryEnum(),
                row.getResourceType(),
                row.getDeliveryType(),
                row.getPublicId(),
                row.getAssetId(),
                row.getCloudVersion(),
                row.getSecureUrl(),
                row.getOriginalFilename(),
                row.getContentType(),
                orZero(row.getByteSize()),
                row.getWidth(),
                row.getHeight(),
                row.getChecksumSha256(),
                orZero(row.getUploadedBy()),
                new MediaOwnerRef(row.getOwnerKindEnum(), orZero(row.getOwnerId())),
                row.getStatus(),
                row.getCreateTime() == null ? null : row.getCreateTime().toInstant(ZoneOffset.UTC));
    }

    private static long orZero(Long value) {
        return value == null ? 0L : value;
    }
}
