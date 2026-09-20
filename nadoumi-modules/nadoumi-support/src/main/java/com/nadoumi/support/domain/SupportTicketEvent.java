package com.nadoumi.support.domain;

import com.nadoumi.support.domain.enums.TicketEventType;
import java.time.LocalDateTime;

/** Row of {@code nad_support_ticket_event}: one append-only work-item state change. */
public class SupportTicketEvent {

    private Long id;
    private Long ticketId;
    private TicketEventType eventType;
    private String oldValue;
    private String newValue;
    private Long actorUserId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public TicketEventType getEventType() { return eventType; }
    public void setEventType(TicketEventType eventType) { this.eventType = eventType; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
