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
        @NotNull ProgramType programType,
        @Size(max = 120) String field,
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
            @Size(max = 200) String nameCn) {
    }

    public record IntakeInput(
            @NotBlank @Size(max = 32) String term,
            LocalDate applicationOpen,
            LocalDate applicationClose) {
    }
}
