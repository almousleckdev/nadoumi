package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationEvent;

public record ApplicationEventResponse(Long id, String eventType, Long actorUserId, String at, String detailJson,
        String actorName, String detailText) {

    /** {@code detailText} is the event's detail in words (an assignee id becomes their name); null when there is none. */
    public static ApplicationEventResponse of(ApplicationEvent e, String actorName, String detailText) {
        return new ApplicationEventResponse(e.getId(), e.getEventType(), e.getActorUserId(),
                e.getAt() == null ? null : e.getAt().toString(), e.getDetailJson(), actorName, detailText);
    }
}
