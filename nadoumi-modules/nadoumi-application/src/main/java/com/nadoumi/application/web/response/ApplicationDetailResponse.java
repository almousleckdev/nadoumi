package com.nadoumi.application.web.response;

import java.util.List;

/**
 * Full staff-side view: application + current tasks + history + events + decisions.
 * Staff-only — never returned to a student caller (that is {@link StudentApplicationResponse}).
 */
public record ApplicationDetailResponse(
        ApplicationResponse application,
        List<TaskResponse> tasks,
        List<StageHistoryResponse> history,
        List<ApplicationEventResponse> events,
        List<DecisionResponse> decisions,
        ApplicantSummary applicant,
        OpportunitySummary opportunity,
        String assigneeName) {

    /** Who applied, as far as a case officer needs at a glance; the full profile is on the applicant page. */
    public record ApplicantSummary(Long id, String publicId, String name, String email, String phone, String nationality) {
    }

    /** What the application is for. {@code universityName} is staff-only and null for scholarship applications. */
    public record OpportunitySummary(String kind, String title, String universityName, String intake) {
    }
}
