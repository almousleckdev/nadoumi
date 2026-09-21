package com.nadoumi.communication.web.response;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
        long id,
        String subject,
        Long applicationId,
        String conversationType,
        String status,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        long unreadCount) {
}
