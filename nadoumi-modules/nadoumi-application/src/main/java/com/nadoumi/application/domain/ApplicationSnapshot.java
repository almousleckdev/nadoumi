package com.nadoumi.application.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_application_snapshot} — immutable, written once on submit (DA4). */
public class ApplicationSnapshot {

    private Long id;
    private Long applicationId;
    private String kind; // PROFILE | REQUIREMENTS
    private String payloadJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
