package com.nadoumi.application.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_application_stage_history} — append-only, never updated or deleted. */
public class ApplicationStageHistory {

    private Long id;
    private Long applicationId;
    private Long fromStageId;
    private Long toStageId;
    private String transitionCode;
    private Long changedBy;
    private LocalDateTime changedAt;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getFromStageId() { return fromStageId; }
    public void setFromStageId(Long fromStageId) { this.fromStageId = fromStageId; }

    public Long getToStageId() { return toStageId; }
    public void setToStageId(Long toStageId) { this.toStageId = toStageId; }

    public String getTransitionCode() { return transitionCode; }
    public void setTransitionCode(String transitionCode) { this.transitionCode = transitionCode; }

    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
