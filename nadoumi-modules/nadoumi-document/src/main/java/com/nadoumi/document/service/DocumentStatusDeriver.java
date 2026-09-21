package com.nadoumi.document.service;

import com.nadoumi.document.domain.DocumentVersion;
import com.nadoumi.document.domain.enums.DocumentStatus;
import java.time.LocalDate;

/**
 * {@code nad_document.status} is derived from its current version's verification
 * outcome plus expiry (DOCUMENT_MANAGEMENT.md INV10) — never set directly by a
 * caller. The first-ever version lands as {@code SUBMITTED}; any later version
 * (a re-upload after rejection) moves the document back to {@code IN_REVIEW}.
 */
final class DocumentStatusDeriver {

    private DocumentStatusDeriver() {
    }

    static DocumentStatus onNewVersion(DocumentVersion version) {
        return version.getVersionNo() == 1 ? DocumentStatus.SUBMITTED : DocumentStatus.IN_REVIEW;
    }

    static DocumentStatus onVerified(LocalDate expiresOn) {
        return expiresOn != null && expiresOn.isBefore(LocalDate.now()) ? DocumentStatus.EXPIRED : DocumentStatus.VERIFIED;
    }
}
