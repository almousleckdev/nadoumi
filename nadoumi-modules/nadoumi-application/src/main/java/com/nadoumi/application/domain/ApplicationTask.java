package com.nadoumi.application.domain;

import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import java.time.LocalDateTime;

/**
 * Row of {@code nad_application_task} — a single table for both engine-materialised
 * tasks ({@code wfStageTaskTemplateId} set) and ad-hoc staff-added tasks
 * ({@code wfStageTaskTemplateId} null), per D12.
 */
public class ApplicationTask {

    private Long id;
    private Long applicationId;
    private Long wfStageTaskTemplateId;
    private String title;
    private String roleRequired;
    private boolean mandatory;
    private boolean blocksExit;
    private ApplicationTaskStatus status;
    private Long assigneeUserId;
    private LocalDateTime dueAt;
    private String skipReason;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getWfStageTaskTemplateId() { return wfStageTaskTemplateId; }
    public void setWfStageTaskTemplateId(Long wfStageTaskTemplateId) { this.wfStageTaskTemplateId = wfStageTaskTemplateId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public boolean isBlocksExit() { return blocksExit; }
    public void setBlocksExit(boolean blocksExit) { this.blocksExit = blocksExit; }

    public ApplicationTaskStatus getStatus() { return status; }
    public void setStatus(ApplicationTaskStatus status) { this.status = status; }

    public Long getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
