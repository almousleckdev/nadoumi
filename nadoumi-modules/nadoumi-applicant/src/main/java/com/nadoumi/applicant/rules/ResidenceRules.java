package com.nadoumi.applicant.rules;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EducationLevel;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;

/** What a student currently living in China must tell us, and that the visa is still valid. */
public final class ResidenceRules {

    public static final String CHINA = "CN";

    private ResidenceRules() {
    }

    public static void validate(boolean inChina, String country, EducationLevel chinaEducationLevel, ChinaVisaType visaType,
            LocalDate visaExpiry, LocalDate today) {
        if (inChina && !CHINA.equals(country)) {
            throw new NadBadRequestException("a student in China must have China as the country");
        }
        if (!inChina && CHINA.equals(country)) {
            throw new NadBadRequestException("choose 'in China' when the country is China");
        }
        if (!inChina) {
            return;
        }
        if (chinaEducationLevel == null || visaType == null || visaExpiry == null) {
            throw new NadBadRequestException("education level, visa type and visa expiry are required in China");
        }
        if (!visaExpiry.isAfter(today)) {
            throw new NadBadRequestException("the visa has expired; it must expire after today");
        }
    }
}
