/**
 * Scholarship slice of the modular monolith. {@code nad_scholarship} (+ levels,
 * categories, intakes, eligibility, fees, stipend, document requirements) is
 * STUDENT-SAFE; the university / partnership / commission linkage lives only in
 * {@code nad_scholarship_internal}. Public discovery reads
 * {@code v_scholarship_student} exclusively; the internal table is reachable
 * only through {@code nad:scholarship:internal:*}-checked staff endpoints.
 */
package com.nadoumi.scholarship;
