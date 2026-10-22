package com.nadoumi.document.domain;

import com.nadoumi.document.domain.enums.RequirementScope;
import java.time.LocalDateTime;

/** Row of {@code nad_document_template}. */
public class DocumentTemplate {

    private Long id;
    private RequirementScope scope;
    private Long refId;
    private String title;
    private String fileName;
    private Long mediaId;
    private String createBy;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RequirementScope getScope() { return scope; }
    public void setScope(RequirementScope scope) { this.scope = scope; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
