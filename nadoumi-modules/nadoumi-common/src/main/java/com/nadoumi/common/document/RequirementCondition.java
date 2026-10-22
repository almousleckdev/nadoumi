package com.nadoumi.common.document;

/**
 * When a required document applies. A requirement with no condition always applies; otherwise it is on a student's
 * checklist only when the condition holds for that student (see the application module's evaluator).
 */
public enum RequirementCondition {
    /** The applicant is under 18. */
    MINOR,
    /** The applicant is in China now. */
    IN_CHINA,
    /** The applicant has studied in China (or studies there now). */
    STUDIED_IN_CHINA,
    /** The study level is a master's or a doctorate. */
    MASTER_OR_DOCTORAL,
    /** The study level is a doctorate. */
    DOCTORAL;

    /** Parses a stored code; blank means "always" and an unknown code is refused. */
    public static RequirementCondition ofOrNull(String code) {
        return code == null || code.isBlank() ? null : valueOf(code.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
