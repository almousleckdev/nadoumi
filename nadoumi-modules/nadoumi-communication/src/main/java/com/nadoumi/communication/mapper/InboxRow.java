package com.nadoumi.communication.mapper;

import java.time.LocalDateTime;

/** One conversation as the caller sees it, read in a single query (summary, unread count and the other side). */
public record InboxRow(
        long conversationId,
        String subject,
        Long applicationId,
        String conversationType,
        String status,
        Long lastMessageId,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        Long lastSenderUserId,
        long unreadCount,
        Long peerUserId,
        Long peerDeliveredMessageId,
        Long peerReadMessageId) {
}
