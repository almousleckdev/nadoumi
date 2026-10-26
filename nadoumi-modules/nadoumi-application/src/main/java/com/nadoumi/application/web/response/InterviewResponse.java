package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationInterview;
import java.time.ZoneOffset;

/** What both staff and the student may read about an interview; {@code startsAt} is an ISO instant in UTC. */
public record InterviewResponse(
        Long id, String startsAt, int durationMinutes, String platform, String meetingUrl, String instructions,
        String status) {

    public static InterviewResponse of(ApplicationInterview i) {
        return new InterviewResponse(i.getId(), i.getStartsAt().toInstant(ZoneOffset.UTC).toString(),
                i.getDurationMinutes(), i.getPlatform(), i.getMeetingUrl(), i.getInstructions(), i.getStatus());
    }
}
