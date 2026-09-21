package com.nadoumi.document.web.response;

import com.nadoumi.document.domain.DocumentEvent;
import java.time.LocalDateTime;

public record DocumentEventResponse(long id, String eventType, long actorUserId, LocalDateTime at, String detail) {

    public static DocumentEventResponse from(DocumentEvent e) {
        return new DocumentEventResponse(e.getId(), e.getEventType(), e.getActorUserId(), e.getAt(), e.getDetailJson());
    }
}
