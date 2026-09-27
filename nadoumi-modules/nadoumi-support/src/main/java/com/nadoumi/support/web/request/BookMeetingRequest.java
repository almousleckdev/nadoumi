package com.nadoumi.support.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class BookMeetingRequest {

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private Integer durationMinutes;

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}
