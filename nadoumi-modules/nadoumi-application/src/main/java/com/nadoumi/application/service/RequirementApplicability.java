package com.nadoumi.application.service;

import com.nadoumi.common.document.RequirementCondition;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

/**
 * Decides whether a conditional document requirement applies to a student. A fact that is not known yet (no date of
 * birth, no location answer, no level) counts as "applies", so a document is asked for rather than silently skipped.
 */
final class RequirementApplicability {

    /** What is known about the student and the opportunity; {@code null} means unknown. */
    record Facts(LocalDate dateOfBirth, Boolean inChina, Boolean studiedInChina, String level) {
    }

    static final int ADULT_AGE = 18;
    private static final Set<String> MASTER_OR_ABOVE = Set.of("MASTER", "DOCTORATE", "DOCTORAL", "PHD");
    private static final Set<String> DOCTORAL = Set.of("DOCTORATE", "DOCTORAL", "PHD");

    private RequirementApplicability() {
    }

    static boolean applies(RequirementCondition condition, Facts facts, LocalDate today) {
        if (condition == null) {
            return true;
        }
        return switch (condition) {
            case MINOR -> facts.dateOfBirth() == null || Period.between(facts.dateOfBirth(), today).getYears() < ADULT_AGE;
            case IN_CHINA -> facts.inChina() == null || facts.inChina();
            case STUDIED_IN_CHINA -> facts.studiedInChina() == null || facts.studiedInChina();
            case MASTER_OR_DOCTORAL -> facts.level() == null || MASTER_OR_ABOVE.contains(facts.level());
            case DOCTORAL -> facts.level() == null || DOCTORAL.contains(facts.level());
        };
    }

    /** The highest study level among the candidates, upper-cased; null when there are none. */
    static String highestLevel(List<String> levels) {
        String best = null;
        int bestRank = -1;
        for (String raw : levels) {
            if (raw == null) {
                continue;
            }
            String level = raw.strip().toUpperCase(java.util.Locale.ROOT);
            int rank = DOCTORAL.contains(level) ? 3 : "MASTER".equals(level) ? 2 : "BACHELOR".equals(level) ? 1 : 0;
            if (rank > bestRank) {
                best = level;
                bestRank = rank;
            }
        }
        return best;
    }
}
