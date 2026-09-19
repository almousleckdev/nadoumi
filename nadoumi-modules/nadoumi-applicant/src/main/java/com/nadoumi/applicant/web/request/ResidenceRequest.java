package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EducationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Where the applicant is now. The China fields apply only when {@code inChina}. */
public record ResidenceRequest(
        @NotNull
        Boolean inChina,
        @NotBlank @Size(min = 2, max = 2)
        String country,
        @NotBlank @Size(max = 80)
        String city,
        @Size(max = 255)
        String address,
        EducationLevel chinaEducationLevel,
        @Size(max = 200)
        String chinaSchool,
        ChinaVisaType visaType,
        LocalDate visaExpiryDate) {
}
