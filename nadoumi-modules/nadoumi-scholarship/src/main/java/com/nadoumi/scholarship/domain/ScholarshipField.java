package com.nadoumi.scholarship.domain;

/** A field of study a scholarship covers. {@code level} is an education level code, or null for every level. */
public record ScholarshipField(String level, String name) {
}
