package com.nadoumi.application.service;

import com.nadoumi.application.domain.enums.ApplicationType;
import com.nadoumi.common.exception.NadBadRequestException;

/** Which opportunity columns each application type must and must not carry. One rule, used on create and edit. */
final class ApplicationShape {

    private ApplicationShape() {
    }

    static void validate(ApplicationType type, Long programId, Long scholarshipId, Long intakeId) {
        switch (type) {
            case PROGRAM_ONLY -> {
                require(programId != null, "a PROGRAM_ONLY application requires a program_id");
                require(scholarshipId == null, "a PROGRAM_ONLY application may not carry a scholarship_id");
            }
            case PROGRAM_WITH_SCHOLARSHIP -> {
                require(programId != null, "a PROGRAM_WITH_SCHOLARSHIP application requires a program_id");
                require(scholarshipId != null, "a PROGRAM_WITH_SCHOLARSHIP application requires a scholarship_id");
            }
            case SCHOLARSHIP_ONLY -> {
                require(scholarshipId != null, "a SCHOLARSHIP_ONLY application requires a scholarship_id");
                require(programId == null, "a SCHOLARSHIP_ONLY application may not carry a program_id");
                require(intakeId == null, "a SCHOLARSHIP_ONLY application may not carry an intake_id");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new NadBadRequestException(message);
        }
    }
}
