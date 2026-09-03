package com.nadoumi.scholarship.domain;

import java.math.BigDecimal;

/** A typed monetary line item. {@code amount} is exact; never a float. */
public record ScholarshipFee(String kind, BigDecimal amount, String currency, String note) {
}
