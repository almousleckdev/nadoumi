package com.nadoumi.program.web.response;

import com.nadoumi.program.domain.Program;
import java.math.BigDecimal;
import java.util.List;

/**
 * Student-safe programme view -- the only shape returned by
 * {@code /api/public/programs} and {@code /api/public/universities/{id}/programs}.
 * Only PUBLISHED + ACTIVE programmes of a PUBLISHED + ACTIVE university are ever
 * mapped to this. No operational status, no audit, no internal notes. List rows
 * leave {@code majors} / {@code intakes} null; the detail endpoint fills them.
 */
public record PublicProgramResponse(
        Long id,
        Long universityId,
        String universityName,
        String name,
        String nameCn,
        String programType,
        String field,
        String teachingLanguage,
        Integer durationMonths,
        BigDecimal tuitionAmount,
        String tuitionCurrency,
        String summary,
        boolean featured,
        boolean hot,
        List<ProgramResponse.Major> majors,
        List<ProgramResponse.Intake> intakes) {

    public static PublicProgramResponse card(Program p) {
        return build(p, false);
    }

    public static PublicProgramResponse detail(Program p) {
        return build(p, true);
    }

    private static PublicProgramResponse build(Program p, boolean detail) {
        ProgramResponse full = ProgramResponse.of(p);
        return new PublicProgramResponse(
                full.id(), full.universityId(), full.universityName(),
                full.name(), full.nameCn(), full.programType(), full.field(),
                full.teachingLanguage(), full.durationMonths(),
                full.tuitionAmount(), full.tuitionCurrency(), full.summary(),
                full.featured(), full.hot(),
                detail ? full.majors() : null,
                detail ? full.intakes() : null);
    }
}
