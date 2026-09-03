package com.nadoumi.scholarship.mapper;

import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.domain.enums.ScholarshipStatus;
import com.nadoumi.scholarship.domain.enums.TeachingLanguage;
import java.time.LocalDate;
import java.util.List;

/**
 * Filter for {@link ScholarshipMapper#searchPublic} / {@code searchStaff}. Every
 * field is optional. {@code publishStatus} / {@code status} apply to the staff
 * search only — the public search reads {@code v_scholarship_student}, which is
 * already restricted to PUBLISHED + ACTIVE.
 *
 * @param sort one of {@code deadline}, {@code newest}, {@code title} (default: relevance)
 */
public record ScholarshipSearch(
        String q,
        String country,
        String province,
        String city,
        String field,
        TeachingLanguage teachingLanguage,
        FundingModel fundingModel,
        Boolean hasStipend,
        LocalDate deadlineBefore,
        List<String> levels,
        List<String> categoryCodes,
        List<String> intakeTerms,
        Boolean featured,
        Boolean recommended,
        Boolean hot,
        String sort,
        PublishStatus publishStatus,
        ScholarshipStatus status) {

    public boolean joinsLevels() {
        return levels != null && !levels.isEmpty();
    }

    public boolean joinsCategories() {
        return categoryCodes != null && !categoryCodes.isEmpty();
    }

    public boolean joinsIntakes() {
        return intakeTerms != null && !intakeTerms.isEmpty();
    }
}
