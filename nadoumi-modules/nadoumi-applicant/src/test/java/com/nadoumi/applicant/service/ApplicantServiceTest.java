package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.request.SelfApplicantRequest;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.ruoyi.framework.web.service.PermissionService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

class ApplicantServiceTest {

    private static final long ID = 5L;

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final UserApplicantAccessService grants = mock(UserApplicantAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PermissionService rbac = mock(PermissionService.class);
    private final ApplicantService service = new ApplicantService(mapper, access, grants, caller, rbac, new ApplicantAccessGuard(access));

    private static Applicant saved() {
        Applicant a = new Applicant();
        a.setId(ID);
        a.setEmail("a@example.com");
        return a;
    }

    private static SelfApplicantRequest request(String given, LocalDate dob, String email) {
        return new SelfApplicantRequest(given, "hassan", dob, "eg", null, email, "+8613800000000",
                "FEMALE", "eg", "cn", "AR", null, "+8613800000000");
    }

    private void studentEditing(Applicant current) {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(caller.requireUserId()).thenReturn(1L);
        when(caller.isExternal()).thenReturn(true);
        when(mapper.findById(ID)).thenReturn(current);
    }

    @Test
    void shouldStoreUppercaseNamesAndCodes_whenAStudentSavesTheirProfile() {
        studentEditing(saved());

        service.update(ID, request("  mary  ann ", LocalDate.now().minusYears(20), "a@example.com"));

        ArgumentCaptor<Applicant> stored = forClass(Applicant.class);
        verify(mapper).update(stored.capture());
        assertThat(stored.getValue().getGivenName()).isEqualTo("MARY ANN");
        assertThat(stored.getValue().getFamilyName()).isEqualTo("HASSAN");
        assertThat(stored.getValue().getNationality()).isEqualTo("EG");
        assertThat(stored.getValue().getCountryOfResidence()).isEqualTo("CN");
        assertThat(stored.getValue().getNativeLanguage()).isEqualTo("ar");
    }

    @Test
    void shouldRejectTheSave_whenDateOfBirthIsUnderSeventeen() {
        studentEditing(saved());

        assertThatThrownBy(() -> service.update(ID, request("mary", LocalDate.now().minusYears(16), "a@example.com")))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectTheSave_whenAStudentChangesTheEmailWithoutVerifyingIt() {
        studentEditing(saved());

        assertThatThrownBy(() -> service.update(ID, request("mary", LocalDate.now().minusYears(20), "other@example.com")))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("verify");
        verify(mapper, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAllowStaffToChangeTheEmail() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(caller.requireUserId()).thenReturn(2L);
        when(caller.isExternal()).thenReturn(false);
        when(mapper.findById(ID)).thenReturn(saved());

        service.update(ID, request("mary", LocalDate.now().minusYears(20), "other@example.com"));

        verify(mapper).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRefuseTheSave_whenCallerCannotEditThisApplicant() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.update(ID, request("mary", LocalDate.now().minusYears(20), "a@example.com")))
                .isInstanceOf(AccessDeniedException.class);
    }
}
