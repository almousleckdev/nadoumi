package com.nadoumi.document.domain.enums;

/** Malware-scan state of one {@code nad_document_version}. Scanning itself is not yet built (column reserved). */
public enum ScanStatus {
    PENDING,
    CLEAN,
    INFECTED
}
