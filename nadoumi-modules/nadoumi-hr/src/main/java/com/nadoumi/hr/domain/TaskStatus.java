package com.nadoumi.hr.domain;

import java.util.Map;
import java.util.Set;

/**
 * Task lifecycle. Allowed transitions:
 * <pre>
 *   PENDING     -> IN_PROGRESS, CANCELLED
 *   IN_PROGRESS -> COMPLETED, PENDING, CANCELLED
 *   COMPLETED   -> CLOSED (admin only), IN_PROGRESS (reopen), CANCELLED
 *   CLOSED      -> (terminal)
 *   CANCELLED   -> (terminal)
 * </pre>
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CLOSED,
    CANCELLED;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = Map.of(
            PENDING, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED, PENDING, CANCELLED),
            COMPLETED, Set.of(CLOSED, IN_PROGRESS, CANCELLED),
            CLOSED, Set.of(),
            CANCELLED, Set.of());

    public boolean canMoveTo(TaskStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }
}
