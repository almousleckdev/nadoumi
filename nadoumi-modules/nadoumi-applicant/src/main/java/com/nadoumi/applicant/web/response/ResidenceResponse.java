package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantResidence;
import java.time.LocalDate;

public record ResidenceResponse(
        boolean inChina,
        String country,
        String city,
        String address,
        String chinaEducationLevel,
        String chinaSchool,
        String visaType,
        LocalDate visaExpiryDate) {

    public static ResidenceResponse of(ApplicantResidence r) {
        return new ResidenceResponse(r.isInChina(), r.getCountry(), r.getCity(), r.getAddress(),
                r.getChinaEducationLevel() == null ? null : r.getChinaEducationLevel().name(), r.getChinaSchool(),
                r.getVisaType() == null ? null : r.getVisaType().name(), r.getVisaExpiryDate());
    }
}
