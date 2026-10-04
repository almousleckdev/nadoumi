package com.nadoumi.notification.domain;

/**
 * Declares whether a notification is broadcast to the platform audience (e.g. catalog announcements)
 * or strictly targeted to explicit recipients (e.g. welcome, personal application updates, messages).
 */
public enum NotificationScope {
    /** Broad platform announcements: all active students and staff holding catalogue view. */
    GLOBAL,
    /** Announcements for students only: every active registered student, no staff. */
    STUDENTS,
    /** Strictly targeted notification: must have explicit intended recipient(s). Never broadcasts. */
    TARGETED
}
