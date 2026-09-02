package com.nadoumi.identity.web.response;

import java.util.List;

/** One applicant the current external user can act for, with the effective capabilities. */
public record AccessibleApplicant(Long applicantId, String accessRole, List<String> capabilities) {
}
