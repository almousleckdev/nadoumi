package com.nadoumi.scholarship.domain;

import java.math.BigDecimal;

/** Stipend terms for one accepted education level. */
public record ScholarshipLevelStipend(String level, BigDecimal amount, String currency,
        String frequency, Integer durationMonths, String conditions) {
}
