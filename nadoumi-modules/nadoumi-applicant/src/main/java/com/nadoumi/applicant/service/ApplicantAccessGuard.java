package com.nadoumi.applicant.service;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The one server-side check every applicant-scoped service repeats: the caller must hold the
 * capability on that applicant. Defense in depth on top of the controllers' {@code @PreAuthorize}.
 */
@Component
public class ApplicantAccessGuard {

    private final NadoumiAccessService access;

    public ApplicantAccessGuard(NadoumiAccessService access) {
        this.access = access;
    }

    public void require(long applicantId, ApplicantCapability capability) {
        if (!access.canAccessApplicant(applicantId, capability.name())) {
            throw new AccessDeniedException("missing " + capability + " on applicant " + applicantId);
        }
    }
}
