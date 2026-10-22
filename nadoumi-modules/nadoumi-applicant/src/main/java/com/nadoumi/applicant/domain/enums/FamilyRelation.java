package com.nadoumi.applicant.domain.enums;

/** {@code nad_applicant_family_member.relation}. A father, a mother and a spouse can each be recorded once. */
public enum FamilyRelation {
    FATHER,
    MOTHER,
    SPOUSE,
    SIBLING,
    OTHER;

    /** Relations an applicant has at most one of. */
    public boolean isSingular() {
        return this == FATHER || this == MOTHER || this == SPOUSE;
    }
}
