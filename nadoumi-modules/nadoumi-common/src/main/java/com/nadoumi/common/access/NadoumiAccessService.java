package com.nadoumi.common.access;

/**
 * Resource-scoped authorization for external users (Spring bean {@code @na}).
 *
 * <p>This is the single choke-point for "may this caller act on this applicant /
 * application / document?" checks. It is evaluated live per request against
 * {@code nad_user_applicant_access} (grants are never cached into the Redis
 * {@code LoginUser} session — see {@code docs/SECURITY.md} §2). Staff RBAC
 * ({@code @ss.hasPermi(...)}) is a separate, non-overlapping system.</p>
 *
 * <p>Every method returns {@code true} only when access is allowed. Callers must
 * combine this with a controller-level {@code @PreAuthorize} annotation AND a
 * service-level re-check (defense in depth).</p>
 *
 * <p>Phase 2: interface only. Implementation ships with the identity/applicant slice.</p>
 */
public interface NadoumiAccessService {
    /**
     * @param applicantId target applicant
     * @param capability  one of the {@code nad_user_applicant_access} capabilities,
     *                    e.g. {@code VIEW_PROFILE}, {@code EDIT_PROFILE},
     *                    {@code SUBMIT_APPLICATION}, {@code UPLOAD_DOCUMENT},
     *                    {@code MANAGE_ACCESS}
     * @return true if the current caller holds that capability for the applicant
     */
    boolean canAccessApplicant(Long applicantId, String capability);

    /**
     * @param applicationId target application
     * @param capability    e.g. {@code VIEW_APPLICATION}, {@code SUBMIT_APPLICATION}
     * @return true if the current caller may act on the application with that capability
     */
    boolean canAccessApplication(Long applicationId, String capability);

    /**
     * @return true if the current staff caller may see the confidential
     *         scholarship &rarr; university / partnership linkage
     *         ({@code nad:scholarship:internal:view})
     */
    boolean canViewScholarshipInternal();

    /**
     * @param documentId target document
     * @return true if the current staff caller may verify/reject the document
     */
    boolean canReviewDocument(Long documentId);
}
