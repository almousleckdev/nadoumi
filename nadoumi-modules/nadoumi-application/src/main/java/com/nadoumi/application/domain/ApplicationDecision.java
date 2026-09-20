package com.nadoumi.application.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_application_decision} — append-only, drives {@code DECISION_RECORDED} guards. */
public class ApplicationDecision {

    private Long id;
    private Long applicationId;
    private String decisionType;
    private String outcome;
    private String rationale;
    private String drivesTransitionCode;
    private Long decidedBy;
    private LocalDateTime decidedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getDrivesTransitionCode() { return drivesTransitionCode; }
    public void setDrivesTransitionCode(String drivesTransitionCode) { this.drivesTransitionCode = drivesTransitionCode; }

    public Long getDecidedBy() { return decidedBy; }
    public void setDecidedBy(Long decidedBy) { this.decidedBy = decidedBy; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
