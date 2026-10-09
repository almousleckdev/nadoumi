package com.nadoumi.notification.domain;

/**
 * Declares whether a notification is an announcement to every active student or strictly targeted to
 * explicit recipients (welcome, personal application updates, messages, staff work items). Nothing is
 * ever broadcast to staff: staff hear only about work that needs them.
 */
public enum NotificationScope {
    /** Announcements for students only: every active registered student, no staff. */
    STUDENTS,
    /** Strictly targeted notification: must have explicit intended recipient(s) or a named staff audience. */
    TARGETED
}
