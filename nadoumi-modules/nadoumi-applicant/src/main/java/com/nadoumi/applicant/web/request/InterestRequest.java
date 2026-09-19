package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.IntakeTerm;
import com.nadoumi.applicant.domain.enums.ScholarshipInterest;
import com.nadoumi.applicant.domain.enums.StudyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** What and where the applicant wants to study. */
public record InterestRequest(
        @NotNull
        StudyLevel desiredLevel,
        @NotEmpty @Size(max = 12)
        List<@NotBlank @Size(max = 40) String> fields,
        @NotEmpty @Size(max = 10)
        List<@NotBlank @Size(max = 80) String> cities,
        ScholarshipInterest scholarshipInterest,
        @Min(2020) @Max(2100)
        Integer intakeYear,
        IntakeTerm intakeTerm,
        @Size(max = 8)
        String teachingLanguage,
        @Size(max = 500)
        String notes) {
}
