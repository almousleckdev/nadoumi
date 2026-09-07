package com.nadoumi.hr.domain;

import java.util.Set;

/**
 * Task lifecycle. Allowed transitions:
 * <pre>
 *   PENDING     -> IN_PROGRESS, CANCELLED
 *   IN_PROGRESS -> COMPLETED, PENDING, CANCELLED
 *   COMPLETED   -> APPROVED (admin only), IN_PROGRESS (reopen), CANCELLED
 *   APPROVED    -> (terminal)
 *   CANCELLED   -> (terminal)
 * </pre>
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    APPROVED,
    CANCELLED;

    private static final java.util.Map<TaskStatus, Set<TaskStatus>> ALLOWED = java.util.Map.of(
            PENDING, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED, PENDING, CANCELLED),
            COMPLETED, Set.of(APPROVED, IN_PROGRESS, CANCELLED),
            APPROVED, Set.of(),
            CANCELLED, Set.of());

    public boolean canMoveTo(TaskStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == APPROVED || this == CANCELLED;
    }
}
