package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One job. Work done in China also carries the work visa. */
public record WorkRequest(
        @NotBlank @Size(max = 200)
        String employer,
        @NotBlank @Size(max = 150)
        String jobTitle,
        EmploymentType employmentType,
        @NotBlank @Size(min = 2, max = 2)
        String country,
        @Size(max = 80)
        String city,
        @NotNull
        LocalDate startDate,
        LocalDate endDate,
        /** Still working here; absent means no. */
        Boolean current,
        @Size(max = 1000)
        String description,
        ChinaVisaType workVisaType,
        LocalDate workVisaExpiry) {
}
