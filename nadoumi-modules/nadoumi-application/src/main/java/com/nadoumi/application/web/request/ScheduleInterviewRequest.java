package com.nadoumi.application.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * {@code startsAt} carries its own offset. Staff pick a Beijing wall time, so the admin sends it with {@code +08:00};
 * the server stores the instant in UTC and never guesses a zone.
 */
public record ScheduleInterviewRequest(
        @NotNull OffsetDateTime startsAt,
        @NotNull @Min(10) @Max(480) Integer durationMinutes,
        @Size(max = 60) String platform,
        @NotBlank @Size(max = 500) String meetingUrl,
        @Size(max = 2000) String instructions) {
}
