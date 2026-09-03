package com.nadoumi.scholarship.domain;

import java.math.BigDecimal;

/** Stipend terms. A scholarship has at most one; absence means no stipend. */
public record ScholarshipStipend(BigDecimal amount, String currency, String frequency,
        Integer durationMonths, String conditions) {
}
