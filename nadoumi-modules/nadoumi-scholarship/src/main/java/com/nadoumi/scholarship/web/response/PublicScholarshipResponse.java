package com.nadoumi.scholarship.web.response;

import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * The student-safe scholarship view — the only shape returned by
 * {@code /api/public/scholarships}. Built from {@link Scholarship}, which is
 * itself assembled from {@code v_scholarship_student} and the student-safe child
 * tables. It carries no university / partnership / commission field on any path.
 * List rows leave the detail-only fields null.
 *
 * <p>Every monetary value is presented in both RMB and USD. The stored figure is
 * kept as-is; the other side is computed at a fixed display rate ({@link #RMB_PER_USD}),
 * rounded to whole currency units. This is a display convenience, not an FX quote.
 */
public record PublicScholarshipResponse(
        Long id,
        String slug,
        String referenceCode,
        String title,
        String summary,
        String country,
        String province,
        String city,
        String field,
        String teachingLanguage,
        String fundingModel,
        boolean hasStipend,
        String nonDegreeDuration,
        Integer studyDurationMonths,
        String applicationChannel,
        String agencyNumber,
        boolean requiresFinancialProof,
        boolean requiresFoundationYear,
        LocalDate deadline,
        Money applicationFee,
        Money serviceFee,
        Integer slots,
        boolean featured,
        boolean recommended,
        boolean hot,
        String heroImageUrl,
        String coverImageUrl,
        Long heroMediaId,
        String heroUrl,
        Long coverMediaId,
        String coverUrl,
        List<String> levels,
        List<String> categories,
        List<Intake> intakes,
        // ---- detail only ----
        String benefits,
        String requirements,
        String policy,
        String renewalConditions,
        Eligibility eligibility,
        List<Fee> fees,
        List<LevelStipend> stipends,
        List<Accommodation> accommodation,
        List<Coverage> coverage,
        List<DocumentRequirement> documentRequirements) {

    /** Fixed display rate — CNY per USD. Keep in sync with the admin's guidance. */
    static final BigDecimal RMB_PER_USD = new BigDecimal("7.10");

    public record Money(BigDecimal amountRmb, BigDecimal amountUsd, String currency) {
        static Money of(BigDecimal amount, String currency) {
            if (amount == null) {
                return null;
            }
            String cur = currency == null ? "CNY" : currency.toUpperCase();
            return new Money(rmb(amount, cur), usd(amount, cur), cur);
        }
    }

    public record Intake(String term, LocalDate applicationOpen, LocalDate applicationClose) {
    }

    public record Eligibility(
            Integer ageMin, Integer ageMax, String nationalityScope, String acceptedCountries,
            Boolean inChina, BigDecimal gpaMin, BigDecimal ieltsMin, Integer toeflMin,
            Integer duolingoMin, Integer hskMin, Integer cscaMin, String notes) {
    }

    public record Fee(String kind, BigDecimal amountRmb, BigDecimal amountUsd, String currency, String note) {
        static Fee of(String kind, BigDecimal amount, String currency, String note) {
            String cur = currency == null ? "CNY" : currency.toUpperCase();
            return new Fee(kind, rmb(amount, cur), usd(amount, cur), cur, note);
        }
    }

    public record LevelStipend(String level, BigDecimal amountRmb, BigDecimal amountUsd, String currency,
            String frequency, Integer durationMonths, String conditions) {
    }

    public record Accommodation(String roomType, BigDecimal amountRmb, BigDecimal amountUsd,
            String currency, String note) {
    }

    public record Coverage(String kind, String detail) {
    }

    public record DocumentRequirement(String docType, boolean mandatory, String note) {
    }

    /** Legacy factory — the {@code *_image_url} strings pass through unchanged. */
    public static PublicScholarshipResponse card(Scholarship s) {
        return build(s, false, s.getHeroImageUrl(), s.getCoverImageUrl());
    }

    /** Legacy factory — the {@code *_image_url} strings pass through unchanged. */
    public static PublicScholarshipResponse detail(Scholarship s) {
        return build(s, true, s.getHeroImageUrl(), s.getCoverImageUrl());
    }

    /** The service resolves {@code heroUrl} / {@code coverUrl} via the MediaGateway (legacy fallback). */
    public static PublicScholarshipResponse card(Scholarship s, String heroUrl, String coverUrl) {
        return build(s, false, heroUrl, coverUrl);
    }

    /** The service resolves {@code heroUrl} / {@code coverUrl} via the MediaGateway (legacy fallback). */
    public static PublicScholarshipResponse detail(Scholarship s, String heroUrl, String coverUrl) {
        return build(s, true, heroUrl, coverUrl);
    }

    private static PublicScholarshipResponse build(Scholarship s, boolean detail, String heroUrl, String coverUrl) {
        return new PublicScholarshipResponse(
                s.getId(), s.getSlug(), s.getReferenceCode(), s.getTitle(), s.getSummary(),
                s.getCountry(), s.getProvince(), s.getCity(), s.getField(),
                s.getTeachingLanguage() == null ? null : s.getTeachingLanguage().name(),
                s.getFundingModel() == null ? null : s.getFundingModel().name(),
                s.isHasStipend(), s.getNonDegreeDuration(), s.getStudyDurationMonths(),
                s.getApplicationChannel(), s.getAgencyNumber(),
                s.isRequiresFinancialProof(), s.isRequiresFoundationYear(),
                s.getDeadline(),
                Money.of(s.getApplicationFeeAmount(), s.getApplicationFeeCurrency()),
                Money.of(s.getServiceFeeAmount(), s.getServiceFeeCurrency()),
                s.getSlots(), s.isFeatured(), s.isRecommended(), s.isHot(),
                s.getHeroImageUrl(), s.getCoverImageUrl(),
                s.getHeroMediaId(), heroUrl, s.getCoverMediaId(), coverUrl,
                List.copyOf(s.getLevels()),
                s.getCategories().stream().map(ScholarshipCategory::code).toList(),
                s.getIntakes().stream()
                        .map(i -> new Intake(i.term(), i.applicationOpen(), i.applicationClose())).toList(),
                detail ? s.getBenefits() : null,
                detail ? s.getRequirements() : null,
                detail ? s.getPolicy() : null,
                detail ? s.getRenewalConditions() : null,
                detail ? eligibility(s) : null,
                detail ? s.getFees().stream()
                        .map(f -> Fee.of(f.kind(), f.amount(), f.currency(), f.note())).toList() : null,
                detail ? s.getLevelStipends().stream()
                        .map(st -> new LevelStipend(st.level(), rmb(st.amount(), cur(st.currency())),
                                usd(st.amount(), cur(st.currency())), cur(st.currency()),
                                st.frequency(), st.durationMonths(), st.conditions())).toList() : null,
                detail ? s.getAccommodations().stream()
                        .map(a -> new Accommodation(a.roomType(), rmb(a.amount(), cur(a.currency())),
                                usd(a.amount(), cur(a.currency())), cur(a.currency()), a.note())).toList() : null,
                detail ? s.getCoverage().stream()
                        .map(c -> new Coverage(c.kind(), c.detail())).toList() : null,
                detail ? s.getDocumentRequirements().stream()
                        .map(d -> new DocumentRequirement(d.docType(), d.mandatory(), d.note())).toList() : null);
    }

    private static Eligibility eligibility(Scholarship s) {
        var e = s.getEligibility();
        return e == null ? null : new Eligibility(
                e.ageMin(), e.ageMax(), e.nationalityScope(), e.acceptedCountries(), e.inChina(),
                e.gpaMin(), e.ieltsMin(), e.toeflMin(), e.duolingoMin(), e.hskMin(), e.cscaMin(), e.notes());
    }

    private static String cur(String currency) {
        return currency == null ? "CNY" : currency.toUpperCase();
    }

    private static BigDecimal rmb(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        return "USD".equals(currency)
                ? amount.multiply(RMB_PER_USD).setScale(0, RoundingMode.HALF_UP)
                : amount.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal usd(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        return "USD".equals(currency)
                ? amount.setScale(0, RoundingMode.HALF_UP)
                : amount.divide(RMB_PER_USD, 0, RoundingMode.HALF_UP);
    }
}
