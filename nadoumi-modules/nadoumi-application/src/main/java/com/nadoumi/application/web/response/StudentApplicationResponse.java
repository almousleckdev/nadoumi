package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.WfStage;
import java.util.List;

/**
 * Student-safe view (docs/superpowers spec §II.7): stage/status + a safe timeline.
 * Never internal notes, decision rationales, assignee identity, or SLA data.
 */
public record StudentApplicationResponse(
        Long id,
        String reference,
        Long applicantId,
        String applicationType,
        Long programId,
        Long scholarshipId,
        Long intakeId,
        String currentStageName,
        String currentStatus,
        String submittedAt,
        List<String> timeline,
        /** The programme or scholarship title; the university only for a programme-only application (never for a scholarship). */
        String title,
        String universityName,
        String intake,
        String applicantName,
        /** True when the application is with Nadoumi but its fee is not on record (rejected or deleted by staff): the student must pay again. */
        boolean paymentNeeded,
        /** What staff wrote when they sent the case back for documents; null in every other status. */
        String actionNote) {

    public static StudentApplicationResponse of(Application a, WfStage stage, List<String> timeline, String title,
            String universityName, String intake, String applicantName, boolean paymentNeeded, String actionNote) {
        return new StudentApplicationResponse(
                a.getId(), a.getReference(), a.getApplicantId(), a.getApplicationType() == null ? null : a.getApplicationType().name(),
                a.getProgramId(), a.getScholarshipId(), a.getIntakeId(),
                stage == null ? null : stage.getName(), a.getCurrentStatus(),
                a.getSubmittedAt() == null ? null : a.getSubmittedAt().toString(), timeline, title, universityName, intake,
                applicantName, paymentNeeded, actionNote);
    }
}
