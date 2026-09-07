package com.nadoumi.finance.domain;

import java.util.Set;

/**
 * Expense lifecycle. Transitions:
 * <pre>
 *   DRAFT     -> SUBMITTED, REJECTED
 *   SUBMITTED -> APPROVED, REJECTED, DRAFT
 *   APPROVED  -> PAID, REJECTED
 *   PAID      -> (terminal)
 *   REJECTED  -> DRAFT
 * </pre>
 * The receipt number is assigned on the first move to {@code APPROVED}.
 */
public enum ExpenseStatus {
    DRAFT, SUBMITTED, APPROVED, PAID, REJECTED;

    private static final java.util.Map<ExpenseStatus, Set<ExpenseStatus>> ALLOWED = java.util.Map.of(
            DRAFT, Set.of(SUBMITTED, REJECTED),
            SUBMITTED, Set.of(APPROVED, REJECTED, DRAFT),
            APPROVED, Set.of(PAID, REJECTED),
            PAID, Set.of(),
            REJECTED, Set.of(DRAFT));

    public boolean canMoveTo(ExpenseStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
