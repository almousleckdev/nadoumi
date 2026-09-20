package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.WfStage;

/** List/summary shape — staff and student list endpoints. Never the persistence entity. */
public record ApplicationResponse(
        Long id,
        Long applicantId,
        String applicationType,
        Long programId,
        Long scholarshipId,
        Long intakeId,
        String currentStageCode,
        String currentStageName,
        String currentStatus,
        Long assigneeUserId,
        String submittedAt,
        int version,
        String createdAt) {

    public static ApplicationResponse of(Application a, WfStage stage) {
        return new ApplicationResponse(
                a.getId(), a.getApplicantId(),
                a.getApplicationType() == null ? null : a.getApplicationType().name(),
                a.getProgramId(), a.getScholarshipId(), a.getIntakeId(),
                stage == null ? null : stage.getCode(), stage == null ? null : stage.getName(),
                a.getCurrentStatus(), a.getAssigneeUserId(),
                str(a.getSubmittedAt()), a.getVersion(), str(a.getCreateTime()));
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }
}
