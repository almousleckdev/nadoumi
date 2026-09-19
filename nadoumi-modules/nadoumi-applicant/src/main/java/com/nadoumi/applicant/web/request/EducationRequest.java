package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.EducationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One stage of education, from high school to the applicant's current level. */
public record EducationRequest(
        @NotBlank @Size(max = 200)
        String institution,
        @NotBlank @Size(min = 2, max = 2)
        String country,
        @Size(max = 80)
        String city,
        @NotNull
        EducationLevel level,
        @Size(max = 200)
        String qualification,
        @Size(max = 120)
        String field,
        BigDecimal gpa,
        BigDecimal gpaScale,
        LocalDate startDate,
        LocalDate endDate,
        /** Still studying here; absent means no. */
        Boolean current) {
}
