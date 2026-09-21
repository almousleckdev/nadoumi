package com.nadoumi.document.service;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.document.domain.Document;
import com.nadoumi.document.mapper.DocumentMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The server-side checks every document-scoped operation repeats: the caller must
 * hold the applicant capability (student side) or the review permission (staff
 * side). Defense in depth on top of the controllers' {@code @PreAuthorize} —
 * {@link #ownsDocument} is also the bean the {@code @PreAuthorize} SpEL on
 * by-document-id endpoints calls, since {@link NadoumiAccessService} has no
 * document-scoped resolver the way it does for applications
 * ({@code ApplicationApplicantResolver}); resolving document&rarr;applicant here
 * is the one lookup both layers share.
 */
@Component
public class DocumentAccessGuard {

    private final NadoumiAccessService access;
    private final DocumentMapper documents;

    public DocumentAccessGuard(NadoumiAccessService access, DocumentMapper documents) {
        this.access = access;
        this.documents = documents;
    }

    public void requireApplicant(long applicantId, ApplicantCapability capability) {
        if (!access.canAccessApplicant(applicantId, capability.name())) {
            throw new AccessDeniedException("missing " + capability + " on applicant " + applicantId);
        }
    }

    /** Staff review action (verify/reject) on a specific document. */
    public void requireReview(long documentId) {
        if (!access.canReviewDocument(documentId)) {
            throw new NadForbiddenException("missing document-review permission on document " + documentId);
        }
    }

    /** {@code @PreAuthorize}-callable: does the caller hold {@code capability} on the document's owning applicant? */
    public boolean ownsDocument(long documentId, String capability) {
        Document doc = documents.findById(documentId);
        return doc != null && access.canAccessApplicant(doc.getApplicantId(), capability);
    }
}
