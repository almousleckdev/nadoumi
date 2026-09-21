package com.nadoumi.document.domain;

import java.time.LocalDateTime;

/** Row of {@code nad_document_event} — append-only audit trail. */
public class DocumentEvent {

    private Long id;
    private Long documentId;
    private String eventType;
    private Long actorUserId;
    private LocalDateTime at;
    private String detailJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public LocalDateTime getAt() { return at; }
    public void setAt(LocalDateTime at) { this.at = at; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
}
