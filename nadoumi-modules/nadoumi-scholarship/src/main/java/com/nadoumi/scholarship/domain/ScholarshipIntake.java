package com.nadoumi.scholarship.domain;

import java.time.LocalDate;

/** A start term for a scholarship, with its own application window. */
public record ScholarshipIntake(String term, LocalDate applicationOpen, LocalDate applicationClose) {
}
