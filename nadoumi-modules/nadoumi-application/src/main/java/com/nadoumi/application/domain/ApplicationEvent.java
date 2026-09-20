package com.nadoumi.application.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_application_event} — append-only timeline, never updated or deleted. */
public class ApplicationEvent {

    private Long id;
    private Long applicationId;
    private String eventType;
    private Long actorUserId;
    private LocalDateTime at;
    private String detailJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public LocalDateTime getAt() { return at; }
    public void setAt(LocalDateTime at) { this.at = at; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
}
