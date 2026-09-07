package com.nadoumi.notification.service;

import com.nadoumi.notification.domain.NotificationType;

/**
 * Input to {@link NotificationService#create}. {@code title} / {@code body} are
 * already rendered (the outbox dispatcher resolves the template); {@code dataJson}
 * and the ref ids are safe scalars only ({@code docs/SECURITY.md} §6).
 */
public record NotificationRequest(
        long recipientUserId,
        NotificationType type,
        String title,
        String body,
        String sourceRef,
        Long applicationId,
        Long conversationId,
        Long messageId,
        String dataJson) {

    public static NotificationRequest of(long recipientUserId, NotificationType type, String title, String body) {
        return new NotificationRequest(recipientUserId, type, title, body, null, null, null, null, null);
    }

    /** Event-driven row with an idempotency key and a render/data context. */
    public static NotificationRequest fromEvent(long recipientUserId, NotificationType type, String title,
            String body, String sourceRef, String dataJson) {
        return new NotificationRequest(recipientUserId, type, title, body, sourceRef, null, null, null, dataJson);
    }
}
