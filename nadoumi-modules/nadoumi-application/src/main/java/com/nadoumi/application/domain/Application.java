package com.nadoumi.application.domain;

import com.nadoumi.application.domain.enums.ApplicationType;
import java.time.LocalDateTime;

/**
 * Row of {@code nad_application} — one business case. {@code currentStatus} is
 * denormalised and written only by {@link com.nadoumi.application.service.WorkflowService}
 * (INV9). {@code version} is the optimistic lock.
 */
public class Application {

    private Long id;
    private Long applicantId;
    private ApplicationType applicationType;
    private Long programId;
    private Long scholarshipId;
    private Long intakeId;
    private Long workflowInstanceId;
    private Long currentStageId;
    private String currentStatus;
    private Long assigneeUserId;
    private LocalDateTime submittedAt;
    private int version;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public ApplicationType getApplicationType() { return applicationType; }
    public void setApplicationType(ApplicationType applicationType) { this.applicationType = applicationType; }

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }

    public Long getScholarshipId() { return scholarshipId; }
    public void setScholarshipId(Long scholarshipId) { this.scholarshipId = scholarshipId; }

    public Long getIntakeId() { return intakeId; }
    public void setIntakeId(Long intakeId) { this.intakeId = intakeId; }

    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }

    public Long getCurrentStageId() { return currentStageId; }
    public void setCurrentStageId(Long currentStageId) { this.currentStageId = currentStageId; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public Long getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
