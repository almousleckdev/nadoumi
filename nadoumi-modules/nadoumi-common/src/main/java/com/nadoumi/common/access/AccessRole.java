package com.nadoumi.common.access;

/**
 * Role an external user holds on one applicant via a {@code nad_user_applicant_access}
 * row. Staff are governed by RuoYi RBAC instead. See {@code docs/DOMAIN_MODEL.md} §4.
 */
public enum AccessRole {
    OWNER,
    AGENT,
    GUARDIAN,
    VIEWER
}
