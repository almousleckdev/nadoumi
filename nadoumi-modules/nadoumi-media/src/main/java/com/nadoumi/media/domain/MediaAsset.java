package com.nadoumi.media.domain;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaOwnerKind;
import java.time.LocalDateTime;

/**
 * Row of {@code nad_media_asset} — the provider-reference + metadata registry for
 * one stored media object. Business tables hold a {@code *_media_id} FK to this
 * row and never carry Cloudinary fields themselves.
 *
 * <p>Enum-ish columns ({@code access_class}, {@code category}, {@code owner_kind},
 * {@code status}, {@code resource_type}, {@code delivery_type}) are stored as
 * {@code String} to match the plain-column mapper; {@link #getAccessClassEnum()}
 * and friends give a typed view where callers want one.</p>
 */
public class MediaAsset {

    private Long id;
    private String provider;
    private String accessClass;
    private String category;
    private String resourceType;
    private String deliveryType;
    private String publicId;
    private String assetId;
    private Long cloudVersion;
    private String secureUrl;
    private String folder;
    private String originalFilename;
    private String contentType;
    private Long byteSize;
    private Integer width;
    private Integer height;
    private String checksumSha256;
    private Long uploadedBy;
    private String ownerKind;
    private Long ownerId;
    private String status;
    private Long supersededBy;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private LocalDateTime deletedAt;
    private Long deletedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getAccessClass() { return accessClass; }
    public void setAccessClass(String accessClass) { this.accessClass = accessClass; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public Long getCloudVersion() { return cloudVersion; }
    public void setCloudVersion(Long cloudVersion) { this.cloudVersion = cloudVersion; }

    public String getSecureUrl() { return secureUrl; }
    public void setSecureUrl(String secureUrl) { this.secureUrl = secureUrl; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getByteSize() { return byteSize; }
    public void setByteSize(Long byteSize) { this.byteSize = byteSize; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getOwnerKind() { return ownerKind; }
    public void setOwnerKind(String ownerKind) { this.ownerKind = ownerKind; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getSupersededBy() { return supersededBy; }
    public void setSupersededBy(Long supersededBy) { this.supersededBy = supersededBy; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Long getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }

    // ---- typed views over the stored string codes ----

    /** @return the parsed {@link MediaAccessClass}, or {@code null} when unset. */
    public MediaAccessClass getAccessClassEnum() {
        return accessClass == null ? null : MediaAccessClass.valueOf(accessClass);
    }

    /** @return the parsed {@link MediaCategory}, or {@code null} when unset. */
    public MediaCategory getCategoryEnum() {
        return category == null ? null : MediaCategory.valueOf(category);
    }

    /** @return the parsed {@link MediaOwnerKind}, or {@code null} when unset. */
    public MediaOwnerKind getOwnerKindEnum() {
        return ownerKind == null ? null : MediaOwnerKind.valueOf(ownerKind);
    }
}
