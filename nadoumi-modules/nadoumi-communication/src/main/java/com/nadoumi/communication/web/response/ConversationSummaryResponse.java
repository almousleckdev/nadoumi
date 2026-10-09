package com.nadoumi.communication.web.response;

import java.time.LocalDateTime;

/**
 * One inbox row. The receipt pointers let a client show sent / delivered / read ticks on its own newest
 * messages without another request: a message is delivered once {@code peerDeliveredMessageId >= id}, and read
 * once {@code peerReadMessageId >= id}.
 */
public record ConversationSummaryResponse(
        long id,
        String subject,
        Long applicationId,
        String conversationType,
        String status,
        Long lastMessageId,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        Long lastSenderUserId,
        long unreadCount,
        ChatPerson peer,
        Long peerDeliveredMessageId,
        Long peerReadMessageId) {
}
