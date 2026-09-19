package com.nadoumi.applicant.rules;

import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;

/** Date rules for one education record. An open-ended (current) record has no fixed end. */
public final class EducationRules {

    private EducationRules() {
    }

    public static void validate(LocalDate start, LocalDate end, boolean current, LocalDate today) {
        if (start != null && start.isAfter(today)) {
            throw new NadBadRequestException("education start date cannot be in the future");
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw new NadBadRequestException("education end date cannot be before the start date");
        }
        if (!current && end != null && end.isAfter(today)) {
            throw new NadBadRequestException("a finished education record cannot end in the future");
        }
    }
}
