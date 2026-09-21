package com.nadoumi.support.domain.enums;

/** Kind of work-item state change recorded in {@code nad_support_ticket_event}. */
public enum TicketEventType {
    OPENED,
    STATUS_CHANGED,
    ASSIGNED,
    REASSIGNED,
    PRIORITY_CHANGED,
    CATEGORY_CHANGED
}
