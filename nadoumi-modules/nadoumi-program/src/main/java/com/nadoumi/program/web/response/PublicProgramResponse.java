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
        String universitySlug,
        String name,
        String slug,
        String nameCn,
        String programType,
        List<String> levels,
        String field,
        String termLength,
        String teachingLanguage,
        Integer durationMonths,
        BigDecimal tuitionAmount,
        String tuitionCurrency,
        BigDecimal tuitionAmountUsd,
        String summary,
        Long imageMediaId,
        String imageUrl,
        boolean featured,
        boolean hot,
        List<ProgramResponse.Major> majors,
        List<ProgramResponse.Intake> intakes) {

    public static PublicProgramResponse card(Program p) {
        return build(p, false, null, null);
    }

    public static PublicProgramResponse detail(Program p) {
        return build(p, true, null, null);
    }

    /** The service resolves {@code imageUrl} from {@code image_media_id} via the MediaGateway. */
    public static PublicProgramResponse card(Program p, String imageUrl) {
        return build(p, false, imageUrl, null);
    }

    /** The service resolves {@code imageUrl} from {@code image_media_id} via the MediaGateway. */
    public static PublicProgramResponse detail(Program p, String imageUrl) {
        return build(p, true, imageUrl, null);
    }

    /** {@code tuitionAmountUsd} is computed by the service from the editable FX rate. */
    public static PublicProgramResponse card(Program p, String imageUrl, BigDecimal tuitionAmountUsd) {
        return build(p, false, imageUrl, tuitionAmountUsd);
    }

    /** {@code tuitionAmountUsd} is computed by the service from the editable FX rate. */
    public static PublicProgramResponse detail(Program p, String imageUrl, BigDecimal tuitionAmountUsd) {
        return build(p, true, imageUrl, tuitionAmountUsd);
    }

    private static PublicProgramResponse build(Program p, boolean detail, String imageUrl,
            BigDecimal tuitionAmountUsd) {
        ProgramResponse full = ProgramResponse.of(p, imageUrl, tuitionAmountUsd);
        return new PublicProgramResponse(
                full.id(), full.universityId(), full.universityName(), full.universitySlug(),
                full.name(), full.slug(), full.nameCn(), full.programType(), full.levels(), full.field(),
                full.termLength(), full.teachingLanguage(), full.durationMonths(),
                full.tuitionAmount(), full.tuitionCurrency(), full.tuitionAmountUsd(), full.summary(),
                full.imageMediaId(), full.imageUrl(),
                full.featured(), full.hot(),
                detail ? full.majors() : null,
                detail ? full.intakes() : null);
    }
}
