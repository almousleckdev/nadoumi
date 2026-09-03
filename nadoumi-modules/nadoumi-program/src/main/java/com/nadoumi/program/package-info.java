/**
 * Programme slice of the modular monolith: {@code nad_program} (a course of study
 * offered by exactly one university) and the staff {@code /api/staff/programs} +
 * public {@code /api/public/programs} APIs. A programme belongs to a university
 * and is surfaced only through the university experience -- there is no top-level
 * Programmes product. The owning university is resolved through
 * {@code UniversityService}, never a cross-module SQL join.
 */
package com.nadoumi.program;
