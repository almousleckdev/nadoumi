package com.nadoumi.scholarship.domain;

/** A scholarship category (CSC, provincial, Type A, …). Reference data. */
public record ScholarshipCategory(Long id, String code, String name) {
}
