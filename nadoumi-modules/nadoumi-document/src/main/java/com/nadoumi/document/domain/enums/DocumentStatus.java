package com.nadoumi.document.domain.enums;

/** Lifecycle of a {@code nad_document}, derived from its current version (DOCUMENT_MANAGEMENT.md INV10). */
public enum DocumentStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    VERIFIED,
    REJECTED,
    EXPIRED
}
