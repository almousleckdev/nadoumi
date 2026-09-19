package com.nadoumi.applicant.rules;

import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;

/** Nadoumi accepts applicants aged 17 or over. */
public final class AgeRules {

    public static final int MINIMUM_AGE_YEARS = 17;

    private AgeRules() {
    }

    /** A null date is allowed here; onboarding completion is what makes it mandatory. */
    public static void requireAdultEnough(LocalDate dob, LocalDate today) {
        if (dob == null) {
            return;
        }
        if (dob.isAfter(today)) {
            throw new NadBadRequestException("date of birth cannot be in the future");
        }
        if (!isEligible(dob, today)) {
            throw new NadBadRequestException("applicants must be at least " + MINIMUM_AGE_YEARS + " years old");
        }
    }

    /** True when {@code dob} is present, not in the future, and at least {@link #MINIMUM_AGE_YEARS} years ago. */
    public static boolean isEligible(LocalDate dob, LocalDate today) {
        return dob != null && !dob.isAfter(today.minusYears(MINIMUM_AGE_YEARS));
    }
}
