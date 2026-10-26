package com.nadoumi.application.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_application_letter}. */
public class ApplicationLetter {

    private Long id;
    private Long applicationId;
    private String kind;
    private Long mediaId;
    private String originalFilename;
    private Long uploadedBy;
    private String createBy;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
