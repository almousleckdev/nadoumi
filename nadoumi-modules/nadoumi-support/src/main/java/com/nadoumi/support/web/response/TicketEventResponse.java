package com.nadoumi.support.web.response;

import com.nadoumi.support.domain.SupportTicketEvent;
import java.time.LocalDateTime;

public record TicketEventResponse(
        String eventType,
        String oldValue,
        String newValue,
        long actorUserId,
        LocalDateTime createdAt) {

    public static TicketEventResponse from(SupportTicketEvent e) {
        return new TicketEventResponse(e.getEventType().name(), e.getOldValue(), e.getNewValue(),
                e.getActorUserId(), e.getCreatedAt());
    }
}
