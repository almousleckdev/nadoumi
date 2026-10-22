package com.nadoumi.applicant.rules;

import com.nadoumi.applicant.domain.ApplicantFamilyMember;
import com.nadoumi.applicant.domain.enums.FamilyRelation;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import java.util.List;

/** What the application form requires of the family section. */
public final class FamilyRules {

    /** When a parent has died, the form asks for at least this many other relatives or siblings instead. */
    public static final int RELATIVES_WHEN_A_PARENT_HAS_DIED = 2;

    private FamilyRules() {
    }

    /** One family member on its own: a date of birth cannot be in the future, and a death needs a note. */
    public static void validate(boolean deceased, String note, LocalDate dateOfBirth, LocalDate today) {
        if (dateOfBirth != null && dateOfBirth.isAfter(today)) {
            throw new NadBadRequestException("date of birth cannot be in the future");
        }
        if (deceased && (note == null || note.isBlank())) {
            throw new NadBadRequestException("add a note for a family member who has died");
        }
    }

    /**
     * The whole section is complete when both parents are recorded and, if either has died, at least two
     * siblings or other relatives are recorded as well.
     */
    public static boolean isComplete(List<ApplicantFamilyMember> members) {
        ApplicantFamilyMember father = first(members, FamilyRelation.FATHER);
        ApplicantFamilyMember mother = first(members, FamilyRelation.MOTHER);
        if (father == null || mother == null) {
            return false;
        }
        if (!father.isDeceased() && !mother.isDeceased()) {
            return true;
        }
        long others = members.stream()
                .filter(m -> m.getRelation() == FamilyRelation.SIBLING || m.getRelation() == FamilyRelation.OTHER)
                .count();
        return others >= RELATIVES_WHEN_A_PARENT_HAS_DIED;
    }

    private static ApplicantFamilyMember first(List<ApplicantFamilyMember> members, FamilyRelation relation) {
        return members.stream().filter(m -> m.getRelation() == relation).findFirst().orElse(null);
    }
}
