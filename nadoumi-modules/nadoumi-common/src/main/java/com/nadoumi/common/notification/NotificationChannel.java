package com.nadoumi.common.notification;

/**
 * Delivery SPI for one notification channel (D6). The dispatcher enqueues one
 * {@code nad_notification_delivery} row per enabled channel and calls the matching
 * implementation asynchronously. Body must carry no PII — IDs and safe fields only.
 */
public interface NotificationChannel {
    NotificationChannelKind kind();
    NotificationSendResult send(String recipient, String subject, String body);
}
