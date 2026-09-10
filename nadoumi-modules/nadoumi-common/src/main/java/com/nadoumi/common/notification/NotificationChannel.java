package com.nadoumi.common.notification;

/**
 * Delivery SPI for one notification channel (D6). The dispatcher enqueues one
 * {@code nad_notification_delivery} row per enabled channel and calls the matching
 * implementation asynchronously. Body must carry no PII — IDs and safe fields only.
 */
public interface NotificationChannel {
    NotificationChannelKind kind();

    /**
     * @param recipient channel-specific address (email / user id)
     * @param subject   rendered subject ({@code null} for channels without one)
     * @param body      rendered plain body — IDs and safe fields only, no PII
     * @param type      the {@code NotificationType} name, so a channel can pick a
     *                  destination link or presentation without depending on the
     *                  notification module's enum
     */
    NotificationSendResult send(String recipient, String subject, String body, String type);
}
