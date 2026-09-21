package com.nadoumi.document.web.response;

import com.nadoumi.document.domain.Document;
import java.time.LocalDate;

/**
 * Own-document view. Deliberately excludes {@code reviewerUserId} and any internal
 * event detail — {@code rejectionReason} is the one SHARED-visibility field a
 * student may see (DOCUMENT_MANAGEMENT.md §3.1).
 */
public record StudentDocumentResponse(
        long id,
        Long applicationId,
        String docType,
        String status,
        LocalDate expiresOn,
        String rejectionReason,
        DocumentVersionResponse currentVersion) {

    public static StudentDocumentResponse from(Document d, DocumentVersionResponse currentVersion) {
        String rejectionReason = "REJECTED".equals(d.getStatus().name()) ? d.getRejectionReason() : null;
        return new StudentDocumentResponse(d.getId(), d.getApplicationId(), d.getDocType(), d.getStatus().name(),
                d.getExpiresOn(), rejectionReason, currentVersion);
    }
}
