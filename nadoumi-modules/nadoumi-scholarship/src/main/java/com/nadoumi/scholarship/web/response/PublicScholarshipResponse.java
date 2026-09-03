package com.nadoumi.scholarship.web.response;

import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The student-safe scholarship view — the only shape returned by
 * {@code /api/public/scholarships}. Built from {@link Scholarship}, which is
 * itself assembled from {@code v_scholarship_student} and the student-safe child
 * tables. It carries no university / partnership / commission field on any path.
 * List rows leave the detail-only fields null.
 */
public record PublicScholarshipResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String country,
        String province,
        String city,
        String field,
        String teachingLanguage,
        String fundingModel,
        boolean hasStipend,
        LocalDate deadline,
        Money applicationFee,
        Money serviceFee,
        Integer slots,
        boolean featured,
        boolean recommended,
        boolean hot,
        List<String> levels,
        List<String> categories,
        List<Intake> intakes,
        // ---- detail only ----
        String benefits,
        String requirements,
        String policy,
        Eligibility eligibility,
        List<Fee> fees,
        Stipend stipend,
        List<DocumentRequirement> documentRequirements) {

    public record Money(BigDecimal amount, String currency) {
        static Money of(BigDecimal amount, String currency) {
            return amount == null ? null : new Money(amount, currency);
        }
    }

    public record Intake(String term, LocalDate applicationOpen, LocalDate applicationClose) {
    }

    public record Eligibility(
            Integer ageMin, Integer ageMax, String nationalityScope, String acceptedCountries,
            Boolean inChina, BigDecimal gpaMin, BigDecimal ieltsMin, Integer toeflMin,
            Integer duolingoMin, Integer hskMin, Integer cscaMin, String notes) {
    }

    public record Fee(String kind, BigDecimal amount, String currency, String note) {
    }

    public record Stipend(BigDecimal amount, String currency, String frequency,
            Integer durationMonths, String conditions) {
    }

    public record DocumentRequirement(String docType, boolean mandatory, String note) {
    }

    public static PublicScholarshipResponse card(Scholarship s) {
        return build(s, false);
    }

    public static PublicScholarshipResponse detail(Scholarship s) {
        return build(s, true);
    }

    private static PublicScholarshipResponse build(Scholarship s, boolean detail) {
        return new PublicScholarshipResponse(
                s.getId(), s.getSlug(), s.getTitle(), s.getSummary(),
                s.getCountry(), s.getProvince(), s.getCity(), s.getField(),
                s.getTeachingLanguage() == null ? null : s.getTeachingLanguage().name(),
                s.getFundingModel() == null ? null : s.getFundingModel().name(),
                s.isHasStipend(), s.getDeadline(),
                Money.of(s.getApplicationFeeAmount(), s.getApplicationFeeCurrency()),
                Money.of(s.getServiceFeeAmount(), s.getServiceFeeCurrency()),
                s.getSlots(), s.isFeatured(), s.isRecommended(), s.isHot(),
                List.copyOf(s.getLevels()),
                s.getCategories().stream().map(ScholarshipCategory::code).toList(),
                s.getIntakes().stream()
                        .map(i -> new Intake(i.term(), i.applicationOpen(), i.applicationClose())).toList(),
                detail ? s.getBenefits() : null,
                detail ? s.getRequirements() : null,
                detail ? s.getPolicy() : null,
                detail ? eligibility(s) : null,
                detail ? s.getFees().stream()
                        .map(f -> new Fee(f.kind(), f.amount(), f.currency(), f.note())).toList() : null,
                detail ? stipend(s) : null,
                detail ? s.getDocumentRequirements().stream()
                        .map(d -> new DocumentRequirement(d.docType(), d.mandatory(), d.note())).toList() : null);
    }

    private static Eligibility eligibility(Scholarship s) {
        var e = s.getEligibility();
        return e == null ? null : new Eligibility(
                e.ageMin(), e.ageMax(), e.nationalityScope(), e.acceptedCountries(), e.inChina(),
                e.gpaMin(), e.ieltsMin(), e.toeflMin(), e.duolingoMin(), e.hskMin(), e.cscaMin(), e.notes());
    }

    private static Stipend stipend(Scholarship s) {
        var st = s.getStipend();
        return st == null ? null : new Stipend(
                st.amount(), st.currency(), st.frequency(), st.durationMonths(), st.conditions());
    }
}
