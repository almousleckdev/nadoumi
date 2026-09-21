package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationEvent;

public record ApplicationEventResponse(Long id, String eventType, Long actorUserId, String at, String detailJson) {

    public static ApplicationEventResponse of(ApplicationEvent e) {
        return new ApplicationEventResponse(e.getId(), e.getEventType(), e.getActorUserId(),
                e.getAt() == null ? null : e.getAt().toString(), e.getDetailJson());
    }
}
