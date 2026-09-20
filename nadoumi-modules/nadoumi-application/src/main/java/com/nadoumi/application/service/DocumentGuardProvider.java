package com.nadoumi.application.service;

/**
 * Backs {@code ALL_REQUIRED_DOCUMENTS_ATTACHED} / {@code ALL_REQUIRED_DOCUMENTS_VERIFIED}.
 * No bean exists until the Document module (platform Step 7) ships — {@link
 * com.nadoumi.application.service.GuardEvaluator#activate} rejects any definition
 * that references either predicate while this is unbacked (DA3, fail-closed). The
 * seeded {@code PROGRAM_WITH_SCHOLARSHIP_V1} / {@code PROGRAM_ONLY_V1} definitions
 * gate on a recorded {@code DOCUMENTS_COMPLETE} decision instead.
 */
public interface DocumentGuardProvider {

    boolean allRequiredDocumentsAttached(long applicationId);

    boolean allRequiredDocumentsVerified(long applicationId);
}
