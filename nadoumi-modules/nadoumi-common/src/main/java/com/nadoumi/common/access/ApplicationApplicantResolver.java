package com.nadoumi.common.access;

/**
 * Resolves an application to the applicant it is about, so {@code NadoumiAccessService}
 * can authorize application-scoped calls without depending on the Application module.
 * The Application slice provides the implementation; until then external callers are
 * denied application-scoped access.
 */
public interface ApplicationApplicantResolver {

    /** @return the applicant id for this application, or {@code null} if it does not exist */
    Long applicantIdOf(Long applicationId);
}
