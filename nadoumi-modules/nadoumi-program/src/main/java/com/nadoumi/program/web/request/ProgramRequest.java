package com.nadoumi.program.web.request;

import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.domain.enums.PublishStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Create / update body for {@code /api/staff/programs}. Student-safe fields only. */
public record ProgramRequest(
        @NotNull Long universityId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String nameCn,
        /** DEGREE | LANGUAGE | NON_DEGREE — the kind of programme. */
        @NotNull ProgramType programType,
        /** DIPLOMA / BACHELOR / MASTER / PHD — one or more; DEGREE programmes only. */
        List<String> levels,
        @Size(max = 120) String field,
        /** ONE_SEMESTER | ONE_YEAR — LANGUAGE / NON_DEGREE only; ignored for degree types. */
        @Size(max = 16) String termLength,
        ProgramTeachingLanguage teachingLanguage,
        @Size(max = 4000) String summary,
        @Size(max = 4000) String admissionRequirements,
        /** One required document per line. */
        @Size(max = 4000) String applicationDocuments,
        @Size(max = 4000) String careerProspects,
        @Size(max = 4000) String scholarshipInfo,
        @Min(1) @Max(6) Integer minHsk,
        @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal minIelts,
        @Min(0) @Max(120) Integer minToefl,
        @PositiveOrZero BigDecimal applicationFeeAmount,
        @Size(min = 3, max = 3) String applicationFeeCurrency,
        Long imageMediaId,
        Boolean featured,
        Boolean hot,
        @NotNull ProgramStatus status,
        @NotNull PublishStatus publishStatus,
        @Size(max = 500) String remark,
        @Valid List<MajorInput> majors,
        @Valid List<IntakeInput> intakes,
        /** Per level and duration: tuition and accommodation. Duration / tuition on the programme are derived from these. */
        @Valid List<OfferingInput> offerings,
        /** Codes from the scholarship categories (CSC, PROVINCIAL, ...) that fund the programme; empty or null means none. */
        List<@Size(max = 32) String> scholarshipCategories) {

    public record OfferingInput(
            /** DIPLOMA / BACHELOR / MASTER / PHD for degree programmes; null otherwise. */
            @Size(max = 16) String level,
            @NotNull @Positive @Max(120) Integer durationMonths,
            @PositiveOrZero BigDecimal tuitionAmount,
            @Size(min = 3, max = 3) String tuitionCurrency,
            /** PER_YEAR (default) | PER_SEMESTER | TOTAL. */
            @Size(max = 16) String tuitionPeriod,
            @PositiveOrZero BigDecimal accommodationAmount,
            @Size(min = 3, max = 3) String accommodationCurrency,
            /** PER_MONTH (default) | PER_YEAR | TOTAL. */
            @Size(max = 16) String accommodationPeriod,
            @Size(max = 300) String note) {
    }

    public record MajorInput(
            /** The programme's own title for the major; blank means "use the catalogue title". */
            @Size(max = 200) String name,
            @Size(max = 200) String nameCn,
            /** nad_department.id — must belong to the programme's university (degree types only). */
            Long departmentId,
            /** DIPLOMA / BACHELOR / MASTER / PHD — must be one of the programme's levels. */
            @Size(max = 16) String level,
            /** nad_major.id from the shared catalogue; when absent, {@code name} is matched to (or added to) it. */
            Long majorId) {

        public MajorInput(String name, String nameCn, Long departmentId, String level) {
            this(name, nameCn, departmentId, level, null);
        }
    }

    public record IntakeInput(
            @NotBlank @Size(max = 32) String term,
            LocalDate applicationOpen,
            LocalDate applicationClose) {
    }
}
