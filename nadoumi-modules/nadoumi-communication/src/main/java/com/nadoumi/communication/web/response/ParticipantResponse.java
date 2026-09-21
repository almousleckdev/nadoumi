package com.nadoumi.communication.web.response;

import java.time.LocalDateTime;

public record ParticipantResponse(long userId, String role, LocalDateTime addedAt) {
}
