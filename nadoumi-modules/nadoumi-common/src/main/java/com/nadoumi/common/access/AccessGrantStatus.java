package com.nadoumi.common.access;

/** Lifecycle of a {@code nad_user_applicant_access} grant. Rows are never hard-deleted. */
public enum AccessGrantStatus {
    PENDING,
    ACTIVE,
    REVOKED,
    EXPIRED
}
