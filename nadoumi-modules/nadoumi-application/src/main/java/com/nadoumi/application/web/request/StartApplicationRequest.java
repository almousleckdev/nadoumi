package com.nadoumi.application.web.request;

import com.nadoumi.application.domain.enums.ApplicationType;
import jakarta.validation.constraints.NotNull;

/** {@code programId} is absent for a scholarship-only application. {@code applicantId} is absent on the student/public endpoint — the caller's own applicant is used there. */
public record StartApplicationRequest(
        Long applicantId,
        @NotNull ApplicationType applicationType,
        Long programId,
        Long scholarshipId,
        Long intakeId) {
}
