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
        long unreadCount,
        Long studentUserId,
        String studentName,
        Long adminUserId,
        String adminName) {

    public ConversationSummaryResponse(
            long id,
            String subject,
            Long applicationId,
            String conversationType,
            String status,
            String lastMessagePreview,
            LocalDateTime lastMessageAt,
            long unreadCount) {
        this(id, subject, applicationId, conversationType, status, lastMessagePreview, lastMessageAt, unreadCount,
                null, null, null, null);
    }
}
