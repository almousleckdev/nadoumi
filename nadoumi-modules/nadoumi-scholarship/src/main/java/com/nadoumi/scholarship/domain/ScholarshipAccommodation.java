package com.nadoumi.scholarship.domain;

import java.math.BigDecimal;

/** One accommodation option: a room type, its price and its amenities. */
public record ScholarshipAccommodation(String roomType, BigDecimal amount, String currency, String note) {
}
