package com.nadoumi.application.domain.enums;

/**
 * Opportunity shape an application pursues. Programmes belong to universities; scholarships are standalone and
 * are not linked to a university, so a scholarship can be applied for on its own.
 */
public enum ApplicationType {
    PROGRAM_WITH_SCHOLARSHIP, PROGRAM_ONLY, SCHOLARSHIP_ONLY
}
