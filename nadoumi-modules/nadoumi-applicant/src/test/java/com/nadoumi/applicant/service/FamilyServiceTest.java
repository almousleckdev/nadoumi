package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.ApplicantFamilyMember;
import com.nadoumi.applicant.domain.ApplicantPerson;
import com.nadoumi.applicant.domain.enums.FamilyRelation;
import com.nadoumi.applicant.domain.enums.PersonRole;
import com.nadoumi.applicant.mapper.FamilyMapper;
import com.nadoumi.applicant.web.request.FamilyMemberRequest;
import com.nadoumi.applicant.web.request.PersonRequest;
import com.nadoumi.applicant.web.response.FormReadinessResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

class FamilyServiceTest {

    private static final long ID = 5L;

    private final FamilyMapper mapper = mock(FamilyMapper.class);
    private final ApplicantAccessGuard guard = mock(ApplicantAccessGuard.class);
    private final FamilyService service = new FamilyService(mapper, guard);
    private final FormReadinessService readiness = new FormReadinessService(mapper, guard);

    private static FamilyMemberRequest member(FamilyRelation relation, boolean deceased, String note) {
        return new FamilyMemberRequest(relation, "  Ibrahima Diallo ", null, "Guinea", deceased, note, null, null, null,
                null, "");
    }

    private static ApplicantFamilyMember stored(long id, FamilyRelation relation) {
        ApplicantFamilyMember m = new ApplicantFamilyMember();
        m.setId(id);
        m.setApplicantId(ID);
        m.setRelation(relation);
        return m;
    }

    @Test
    void shouldStoreAMember_withBlankFieldsAsNull() {
        service.addMember(ID, member(FamilyRelation.FATHER, false, null));

        ArgumentCaptor<ApplicantFamilyMember> saved = ArgumentCaptor.forClass(ApplicantFamilyMember.class);
        verify(mapper).insertMember(saved.capture());
        assertThat(saved.getValue().getFullName()).isEqualTo("Ibrahima Diallo");
        assertThat(saved.getValue().getEmail()).isNull();
        assertThat(saved.getValue().getApplicantId()).isEqualTo(ID);
    }

    @Test
    void shouldRefuseASecondFather_butAllowAnotherSibling() {
        when(mapper.findMembers(ID)).thenReturn(List.of(stored(1L, FamilyRelation.FATHER), stored(2L, FamilyRelation.SIBLING)));

        assertThatThrownBy(() -> service.addMember(ID, member(FamilyRelation.FATHER, false, null)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("already recorded");
        service.addMember(ID, member(FamilyRelation.SIBLING, false, null));
        verify(mapper).insertMember(any());
    }

    @Test
    void shouldLetAMemberBeEdited_withoutCountingItselfAsADuplicate() {
        ApplicantFamilyMember father = stored(1L, FamilyRelation.FATHER);
        when(mapper.findMember(1L)).thenReturn(father);
        when(mapper.findMembers(ID)).thenReturn(List.of(father));

        service.updateMember(ID, 1L, member(FamilyRelation.FATHER, false, null));

        verify(mapper).updateMember(father);
    }

    @Test
    void shouldNotTouchAMemberOfAnotherApplicant() {
        ApplicantFamilyMember foreign = stored(9L, FamilyRelation.MOTHER);
        foreign.setApplicantId(99L);
        when(mapper.findMember(9L)).thenReturn(foreign);

        assertThatThrownBy(() -> service.updateMember(ID, 9L, member(FamilyRelation.MOTHER, false, null)))
                .isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).updateMember(any());
    }

    @Test
    void shouldRequireTheEditGrant_toChangeAnything() {
        doThrow(new AccessDeniedException("no grant")).when(guard).require(ID, ApplicantCapability.EDIT_PROFILE);

        assertThatThrownBy(() -> service.addMember(ID, member(FamilyRelation.FATHER, false, null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteMember(ID, 1L)).isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).insertMember(any());
        verify(mapper, never()).deleteMember(1L, ID);
    }

    @Test
    void shouldRequireTheViewGrant_toReadAnything() {
        doThrow(new AccessDeniedException("no grant")).when(guard).require(ID, ApplicantCapability.VIEW_PROFILE);

        assertThatThrownBy(() -> service.members(ID)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.person(ID, PersonRole.SUPPORTER)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> readiness.of(ID)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldInsertThenUpdateASupporter_oneRowPerRole() {
        PersonRequest req = new PersonRequest("Ibrahima Diallo", "Father", "MALE", null, "+224611111111", null, null, null, null);

        service.savePerson(ID, PersonRole.SUPPORTER, req);
        verify(mapper).insertPerson(any());

        when(mapper.findPerson(ID, PersonRole.SUPPORTER)).thenReturn(new ApplicantPerson());
        service.savePerson(ID, PersonRole.SUPPORTER, req);
        verify(mapper).updatePerson(any());
    }

    @Test
    void shouldReportEverythingMissing_forAnEmptyProfile() {
        FormReadinessResponse r = readiness.of(ID);

        assertThat(r.missing()).containsExactly("PERSONAL_DETAILS", "FAMILY", "SUPPORTER", "CHINA_EMERGENCY");
        assertThat(r.complete()).isFalse();
    }

    @Test
    void shouldReportComplete_whenEverythingIsFilled() {
        var personal = new com.nadoumi.applicant.domain.ApplicantPersonal();
        personal.setMaritalStatus(com.nadoumi.applicant.domain.enums.MaritalStatus.SINGLE);
        personal.setReligion("Muslim");
        personal.setPlaceOfBirth("Conakry");
        personal.setPermanentAddress("12 Rue du Port");
        when(mapper.findPersonal(ID)).thenReturn(personal);
        when(mapper.findMembers(ID)).thenReturn(List.of(stored(1L, FamilyRelation.FATHER), stored(2L, FamilyRelation.MOTHER)));
        when(mapper.findPerson(ID, PersonRole.SUPPORTER)).thenReturn(new ApplicantPerson());
        when(mapper.findPerson(ID, PersonRole.CHINA_EMERGENCY)).thenReturn(new ApplicantPerson());

        assertThat(readiness.of(ID).complete()).isTrue();
    }
}
