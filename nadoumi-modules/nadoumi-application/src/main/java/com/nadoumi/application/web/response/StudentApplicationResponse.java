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
        String applicationType,
        Long programId,
        Long scholarshipId,
        Long intakeId,
        String currentStageName,
        String currentStatus,
        String submittedAt,
        List<String> timeline) {

    public static StudentApplicationResponse of(Application a, WfStage stage, List<String> timeline) {
        return new StudentApplicationResponse(
                a.getId(), a.getApplicationType() == null ? null : a.getApplicationType().name(),
                a.getProgramId(), a.getScholarshipId(), a.getIntakeId(),
                stage == null ? null : stage.getName(), a.getCurrentStatus(),
                a.getSubmittedAt() == null ? null : a.getSubmittedAt().toString(), timeline);
    }
}
