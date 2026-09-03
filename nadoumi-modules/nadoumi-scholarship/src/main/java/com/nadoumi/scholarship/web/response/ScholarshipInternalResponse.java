package com.nadoumi.scholarship.web.response;

import com.nadoumi.scholarship.domain.ScholarshipInternal;

/**
 * CONFIDENTIAL. Returned only by {@code GET /api/staff/scholarships/{id}/internal}
 * to holders of {@code nad:scholarship:internal:view}. Never reachable from a
 * public or student path.
 */
public record ScholarshipInternalResponse(
        Long universityId,
        String universityName,
        Long partnershipId,
        String internalStatus,
        String operationalNotes,
        String confidentialTerms,
        String commissionModelJson) {

    public static ScholarshipInternalResponse of(ScholarshipInternal i, String universityName) {
        if (i == null) {
            return new ScholarshipInternalResponse(null, null, null, "DRAFT", null, null, null);
        }
        return new ScholarshipInternalResponse(
                i.universityId(), universityName, i.partnershipId(), i.internalStatus(),
                i.operationalNotes(), i.confidentialTerms(), i.commissionModelJson());
    }
}
