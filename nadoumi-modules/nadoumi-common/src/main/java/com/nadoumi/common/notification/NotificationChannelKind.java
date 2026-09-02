package com.nadoumi.common.notification;

/** Notification delivery channels. Only IN_APP and EMAIL have implementations in v1 (D6). */
public enum NotificationChannelKind {
    IN_APP,
    EMAIL,
    SMS,
    WHATSAPP,
    PUSH
}
