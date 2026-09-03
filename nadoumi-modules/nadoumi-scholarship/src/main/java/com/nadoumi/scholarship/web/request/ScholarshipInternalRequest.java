package com.nadoumi.scholarship.web.request;

import jakarta.validation.constraints.Size;

/**
 * CONFIDENTIAL update body for {@code PUT /api/staff/scholarships/{id}/internal}.
 * {@code partnershipId} is accepted but unused until the Partnership module exists.
 */
public record ScholarshipInternalRequest(
        Long universityId,
        Long partnershipId,
        @Size(max = 24) String internalStatus,
        @Size(max = 20000) String operationalNotes,
        @Size(max = 20000) String confidentialTerms,
        @Size(max = 20000) String commissionModelJson) {
}
