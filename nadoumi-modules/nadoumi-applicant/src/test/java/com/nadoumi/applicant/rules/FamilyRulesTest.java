package com.nadoumi.applicant.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.applicant.domain.ApplicantFamilyMember;
import com.nadoumi.applicant.domain.enums.FamilyRelation;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FamilyRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private static ApplicantFamilyMember member(FamilyRelation relation, boolean deceased) {
        ApplicantFamilyMember m = new ApplicantFamilyMember();
        m.setRelation(relation);
        m.setDeceased(deceased);
        return m;
    }

    @Test
    void shouldRequireANote_whenAFamilyMemberHasDied() {
        assertThatThrownBy(() -> FamilyRules.validate(true, " ", null, TODAY))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("note");
        FamilyRules.validate(true, "Passed away in 2020", null, TODAY);
    }

    @Test
    void shouldRefuseABirthDateInTheFuture() {
        assertThatThrownBy(() -> FamilyRules.validate(false, null, TODAY.plusDays(1), TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldBeIncomplete_withoutBothParents() {
        assertThat(FamilyRules.isComplete(List.of())).isFalse();
        assertThat(FamilyRules.isComplete(List.of(member(FamilyRelation.FATHER, false)))).isFalse();
    }

    @Test
    void shouldBeComplete_withBothParentsAlive() {
        assertThat(FamilyRules.isComplete(List.of(member(FamilyRelation.FATHER, false), member(FamilyRelation.MOTHER, false)))).isTrue();
    }

    @Test
    void shouldNeedTwoRelatives_whenAParentHasDied() {
        var parents = List.of(member(FamilyRelation.FATHER, true), member(FamilyRelation.MOTHER, false));
        assertThat(FamilyRules.isComplete(parents)).isFalse();

        var withOne = List.of(parents.get(0), parents.get(1), member(FamilyRelation.SIBLING, false));
        assertThat(FamilyRules.isComplete(withOne)).isFalse();

        var withTwo = List.of(parents.get(0), parents.get(1), member(FamilyRelation.SIBLING, false), member(FamilyRelation.OTHER, false));
        assertThat(FamilyRules.isComplete(withTwo)).isTrue();
    }

    @Test
    void shouldNotCountASpouseAsARelative() {
        var members = List.of(member(FamilyRelation.FATHER, true), member(FamilyRelation.MOTHER, false),
                member(FamilyRelation.SPOUSE, false), member(FamilyRelation.SIBLING, false));

        assertThat(FamilyRules.isComplete(members)).isFalse();
    }
}
