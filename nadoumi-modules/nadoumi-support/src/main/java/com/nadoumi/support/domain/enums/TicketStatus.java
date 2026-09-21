package com.nadoumi.support.domain.enums;

import java.util.Set;

/**
 * Ticket lifecycle. Allowed moves (design doc §10 #5, default adopted):
 * {@code OPEN -> IN_PROGRESS -> {WAITING_ON_STUDENT <-> IN_PROGRESS} -> RESOLVED -> CLOSED}.
 * {@code CLOSED} is terminal: a student who needs more help opens a new ticket.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_ON_STUDENT,
    RESOLVED,
    CLOSED;

    public Set<TicketStatus> allowedNext() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS);
            case IN_PROGRESS -> Set.of(WAITING_ON_STUDENT, RESOLVED);
            case WAITING_ON_STUDENT -> Set.of(IN_PROGRESS, RESOLVED);
            case RESOLVED -> Set.of(CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canMoveTo(TicketStatus next) {
        return allowedNext().contains(next);
    }
}
