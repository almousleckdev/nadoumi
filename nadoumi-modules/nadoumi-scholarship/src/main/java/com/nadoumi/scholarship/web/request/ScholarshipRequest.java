package com.nadoumi.scholarship.web.request;

import com.nadoumi.scholarship.domain.enums.EducationLevel;
import com.nadoumi.scholarship.domain.enums.FeeKind;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.domain.enums.ScholarshipStatus;
import com.nadoumi.scholarship.domain.enums.StipendFrequency;
import com.nadoumi.scholarship.domain.enums.TeachingLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create / update body for {@code /api/staff/scholarships}. Student-safe fields
 * only. The slug is always derived server-side from {@code title}.
 */
public record ScholarshipRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String summary,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "country must be an ISO alpha-2 code")
        String country,
        @Size(max = 120) String province,
        @Size(max = 120) String city,
        @Size(max = 120) String field,
        TeachingLanguage teachingLanguage,
        @NotNull FundingModel fundingModel,
        Boolean hasStipend,
        LocalDate deadline,
        @Size(max = 20000) String benefits,
        @Size(max = 20000) String requirements,
        @Size(max = 20000) String policy,
        @PositiveOrZero BigDecimal applicationFeeAmount,
        @Size(min = 3, max = 3) String applicationFeeCurrency,
        @PositiveOrZero BigDecimal serviceFeeAmount,
        @Size(min = 3, max = 3) String serviceFeeCurrency,
        @PositiveOrZero Integer slots,
        Boolean featured,
        Boolean recommended,
        Boolean hot,
        @NotNull ScholarshipStatus status,
        @NotNull PublishStatus publishStatus,
        @Size(max = 500) String remark,
        List<@NotNull EducationLevel> levels,
        List<@NotBlank String> categoryCodes,
        @Valid List<IntakeInput> intakes,
        @Valid EligibilityInput eligibility,
        @Valid List<FeeInput> fees,
        @Valid StipendInput stipend,
        @Valid List<DocumentRequirementInput> documentRequirements) {

    public record IntakeInput(@NotBlank @Size(max = 24) String term,
            LocalDate applicationOpen, LocalDate applicationClose) {
    }

    public record EligibilityInput(
            Integer ageMin, Integer ageMax,
            @Size(max = 16) String nationalityScope, @Size(max = 400) String acceptedCountries,
            Boolean inChina, @DecimalMin("0.0") BigDecimal gpaMin, @DecimalMin("0.0") BigDecimal ieltsMin,
            Integer toeflMin, Integer duolingoMin, Integer hskMin, Integer cscaMin,
            @Size(max = 2000) String notes) {
    }

    public record FeeInput(@NotNull FeeKind kind, @NotNull @PositiveOrZero BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency, @Size(max = 200) String note) {
    }

    public record StipendInput(@NotNull @PositiveOrZero BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency, @NotNull StipendFrequency frequency,
            Integer durationMonths, @Size(max = 1000) String conditions) {
    }

    public record DocumentRequirementInput(@NotBlank @Size(max = 48) String docType,
            Boolean mandatory, @Size(max = 400) String note) {

        public boolean mandatoryOrDefault() {
            return mandatory == null || mandatory;
        }
    }
}
