package com.nadoumi.notification.domain;

/** Lifecycle of one {@code nad_notification_delivery} row. */
public enum DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    BOUNCED
}
