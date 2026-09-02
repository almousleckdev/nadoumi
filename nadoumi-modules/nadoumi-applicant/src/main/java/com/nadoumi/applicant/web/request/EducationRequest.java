package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EducationRequest(
        @NotBlank @Size(max = 200)
        String institution,
        @Size(max = 32)
        String level,
        @Size(max = 120)
        String field,
        BigDecimal gpa,
        BigDecimal gpaScale,
        LocalDate startDate,
        LocalDate endDate) {
}
