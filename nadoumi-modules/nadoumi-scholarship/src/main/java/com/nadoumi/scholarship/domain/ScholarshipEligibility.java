package com.nadoumi.scholarship.domain;

import java.math.BigDecimal;

/** Structured, student-safe eligibility. Every threshold is optional. */
public record ScholarshipEligibility(
        Integer ageMin, Integer ageMax,
        String nationalityScope, String acceptedCountries, Boolean inChina,
        BigDecimal gpaMin, BigDecimal ieltsMin, Integer toeflMin, Integer duolingoMin,
        Integer hskMin, Integer cscaMin, String notes) {
}
