package com.nadoumi.common.notification;

/** Result of {@link NotificationChannel#send}. {@code providerMessageId} / {@code error} set per status. */
public record NotificationSendResult(NotificationSendStatus status, String providerMessageId, String error) {

    public static NotificationSendResult sent(String providerMessageId) {
        return new NotificationSendResult(NotificationSendStatus.SENT, providerMessageId, null);
    }

    public static NotificationSendResult failed(String error) {
        return new NotificationSendResult(NotificationSendStatus.FAILED, null, error);
    }
}
