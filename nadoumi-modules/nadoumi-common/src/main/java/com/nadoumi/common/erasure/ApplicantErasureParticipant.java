package com.nadoumi.common.erasure;

/**
 * A module's part in permanently deleting an applicant. The applicant module runs every participant, in
 * {@code @Order}, before it removes the applicant's own rows, so each module deletes the data it owns
 * (applications, documents, ...) without the applicant module knowing those tables exist.
 */
public interface ApplicantErasureParticipant {

    /** Deletes everything this module holds for the applicant, inside the caller's transaction. */
    void erase(long applicantId);
}
