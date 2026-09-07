package com.nadoumi.program.web.request;

import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.domain.enums.PublishStatus;
import jakarta.validation.Valid;
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
        @Positive Integer durationMonths,
        @PositiveOrZero BigDecimal tuitionAmount,
        @Size(min = 3, max = 3) String tuitionCurrency,
        @Size(max = 4000) String summary,
        Long imageMediaId,
        Boolean featured,
        Boolean hot,
        @NotNull ProgramStatus status,
        @NotNull PublishStatus publishStatus,
        @Size(max = 500) String remark,
        @Valid List<MajorInput> majors,
        @Valid List<IntakeInput> intakes) {

    public record MajorInput(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String nameCn,
            /** nad_department.id — must belong to the programme's university (degree types only). */
            Long departmentId,
            /** DIPLOMA / BACHELOR / MASTER / PHD — must be one of the programme's levels. */
            @Size(max = 16) String level) {
    }

    public record IntakeInput(
            @NotBlank @Size(max = 32) String term,
            LocalDate applicationOpen,
            LocalDate applicationClose) {
    }
}
