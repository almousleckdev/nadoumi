package com.nadoumi.program.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.web.request.ProgramRequest.OfferingInput;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validation of a programme's offerings and the "from" summary stored on the programme row. */
final class ProgramOfferingRules {

    /** Shortest accepted duration per degree level, in months (Bachelor 4 years, Master 2, PhD 3). */
    static final Map<String, Integer> MIN_MONTHS_BY_LEVEL = Map.of(
            "DIPLOMA", 12, "BACHELOR", 48, "MASTER", 24, "PHD", 36);

    static final Set<String> TUITION_PERIODS = Set.of("PER_YEAR", "PER_SEMESTER", "TOTAL");
    static final Set<String> ACCOMMODATION_PERIODS = Set.of("PER_MONTH", "PER_YEAR", "TOTAL");

    private ProgramOfferingRules() {
    }

    /** Shortest duration and cheapest tuition across the offerings; stored on {@code nad_program}. */
    record Summary(Integer durationMonths, BigDecimal tuitionAmount, String tuitionCurrency) {
    }

    static void validate(ProgramType type, List<String> programmeLevels, List<OfferingInput> offerings) {
        if (offerings == null) {
            return;
        }
        boolean degree = type != null && type.isDegree();
        Set<String> seen = new HashSet<>();
        for (OfferingInput o : offerings) {
            String level = normalizedLevel(o.level());
            checkLevel(degree, level, programmeLevels);
            checkDuration(level, o.durationMonths());
            checkPeriod(o.tuitionPeriod(), TUITION_PERIODS, "tuition");
            checkPeriod(o.accommodationPeriod(), ACCOMMODATION_PERIODS, "accommodation");
            if (!seen.add(level + "|" + o.durationMonths())) {
                throw new NadBadRequestException("duplicate offering: "
                        + (level == null ? "programme" : level) + " for " + o.durationMonths() + " months");
            }
        }
    }

    static Summary summary(List<OfferingInput> offerings) {
        if (offerings == null || offerings.isEmpty()) {
            return new Summary(null, null, null);
        }
        Integer shortest = offerings.stream().map(OfferingInput::durationMonths).min(Integer::compare).orElse(null);
        OfferingInput cheapest = offerings.stream()
                .filter(o -> o.tuitionAmount() != null)
                .min(Comparator.comparing(OfferingInput::tuitionAmount))
                .orElse(null);
        return new Summary(shortest,
                cheapest == null ? null : cheapest.tuitionAmount(),
                cheapest == null ? null : cheapest.tuitionCurrency());
    }

    static String normalizedLevel(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim().toUpperCase();
    }

    private static void checkLevel(boolean degree, String level, List<String> programmeLevels) {
        if (degree && level == null) {
            throw new NadBadRequestException("each offering of a degree programme needs a level");
        }
        if (!degree && level != null) {
            throw new NadBadRequestException("only degree programmes have levels; remove level " + level);
        }
        if (degree && !programmeLevels.contains(level)) {
            throw new NadBadRequestException("offering level " + level + " is not one of the programme's levels");
        }
    }

    private static void checkDuration(String level, int months) {
        Integer min = level == null ? null : MIN_MONTHS_BY_LEVEL.get(level);
        if (min != null && months < min) {
            throw new NadBadRequestException(level + " duration must be at least " + min + " months");
        }
    }

    private static void checkPeriod(String period, Set<String> allowed, String what) {
        if (period != null && !period.isBlank() && !allowed.contains(period.trim().toUpperCase())) {
            throw new NadBadRequestException("unknown " + what + " period: " + period);
        }
    }
}
