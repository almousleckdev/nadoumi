package com.nadoumi.applicant.rules;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;

/** Date and China work-visa rules for one work-experience record. */
public final class WorkRules {

    private WorkRules() {
    }

    public static void validate(LocalDate start, LocalDate end, boolean current, String country, ChinaVisaType visaType,
            LocalDate today) {
        if (start.isAfter(today)) {
            throw new NadBadRequestException("work start date cannot be in the future");
        }
        if (current && end != null) {
            throw new NadBadRequestException("a current job has no end date");
        }
        if (!current && end == null) {
            throw new NadBadRequestException("a finished job needs an end date");
        }
        if (end != null && (end.isBefore(start) || end.isAfter(today))) {
            throw new NadBadRequestException("work end date must be between the start date and today");
        }
        if (ResidenceRules.CHINA.equals(country) && visaType == null) {
            throw new NadBadRequestException("work in China needs the work visa type");
        }
    }
}
