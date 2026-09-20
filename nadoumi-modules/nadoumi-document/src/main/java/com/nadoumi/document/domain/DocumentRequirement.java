package com.nadoumi.document.domain;

import com.nadoumi.document.domain.enums.RequirementScope;
import java.time.LocalDateTime;

/** Row of {@code nad_document_requirement} — a checklist-definition entry, not a per-application instance. */
public class DocumentRequirement {

    private Long id;
    private RequirementScope scope;
    private Long refId;
    private String docType;
    private boolean mandatory;
    private boolean waivable;
    private String notes;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RequirementScope getScope() { return scope; }
    public void setScope(RequirementScope scope) { this.scope = scope; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public boolean isWaivable() { return waivable; }
    public void setWaivable(boolean waivable) { this.waivable = waivable; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
