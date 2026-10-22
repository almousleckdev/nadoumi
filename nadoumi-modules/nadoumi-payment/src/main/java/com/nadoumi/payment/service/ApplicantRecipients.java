package com.nadoumi.payment.service;

import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.common.access.AccessGrantStatus;
import com.nadoumi.identity.service.UserApplicantAccessService;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

/** The user accounts that act for an applicant: who must be told about a payment decision. */
@Component
public class ApplicantRecipients {

    private final UserApplicantAccessService access;

    public ApplicantRecipients(UserApplicantAccessService access) {
        this.access = access;
    }

    public List<Long> userIdsOf(long applicantId) {
        return List.copyOf(new LinkedHashSet<>(access.listForApplicant(applicantId).stream()
                .filter(g -> g.getStatus() == AccessGrantStatus.ACTIVE)
                .map(UserApplicantAccess::getUserId)
                .toList()));
    }
}
