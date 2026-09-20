package com.nadoumi.document.web.response;

import com.nadoumi.document.domain.Document;
import java.time.LocalDate;
import java.util.List;

/** Full staff view: reviewer, complete version history, event log. Gated by {@code nad:document:view}. */
public record StaffDocumentResponse(
        long id,
        long applicantId,
        Long applicationId,
        String docType,
        String status,
        LocalDate expiresOn,
        Long reviewerUserId,
        String rejectionReason,
        List<DocumentVersionResponse> versions,
        List<DocumentEventResponse> events) {

    public static StaffDocumentResponse from(Document d, List<DocumentVersionResponse> versions,
            List<DocumentEventResponse> events) {
        return new StaffDocumentResponse(d.getId(), d.getApplicantId(), d.getApplicationId(), d.getDocType(),
                d.getStatus().name(), d.getExpiresOn(), d.getReviewerUserId(), d.getRejectionReason(), versions, events);
    }
}
