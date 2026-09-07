package com.nadoumi.notification.web.response;

import com.nadoumi.notification.domain.Notification;
import java.time.LocalDateTime;

/**
 * Recipient-facing view of a notification. Every field is already safe — no
 * internal identifiers beyond the resource refs the client needs to navigate.
 */
public record NotificationView(
        long id,
        String type,
        String title,
        String body,
        String dataJson,
        Long applicationId,
        Long conversationId,
        Long messageId,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean read) {

    public static NotificationView from(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getDataJson(),
                n.getApplicationId(), n.getConversationId(), n.getMessageId(),
                n.getCreatedAt(), n.getReadAt(), n.getReadAt() != null);
    }
}
