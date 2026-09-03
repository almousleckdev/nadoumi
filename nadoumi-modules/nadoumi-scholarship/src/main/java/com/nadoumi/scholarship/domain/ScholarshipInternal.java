package com.nadoumi.scholarship.domain;

/**
 * CONFIDENTIAL row of {@code nad_scholarship_internal}. The partner university /
 * partnership linkage and operational terms. Never reachable from a public or
 * student code path; only {@code nad:scholarship:internal:*} holders.
 */
public record ScholarshipInternal(
        Long universityId, Long partnershipId, String internalStatus,
        String operationalNotes, String confidentialTerms, String commissionModelJson) {
}
