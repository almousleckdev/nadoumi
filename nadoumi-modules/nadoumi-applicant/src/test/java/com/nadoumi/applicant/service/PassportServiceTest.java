package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.request.PassportRequest;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.identity.access.CurrentCaller;
import com.ruoyi.framework.web.service.PermissionService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

class PassportServiceTest {

    private static final long ID = 5L;
    private static final LocalDate DOB = LocalDate.of(2000, 5, 1);

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PermissionService rbac = mock(PermissionService.class);
    private final PassportService service = new PassportService(mapper, access, caller, rbac);

    private static Applicant profile() {
        Applicant a = new Applicant();
        a.setId(ID);
        a.setGivenName("AHMED");
        a.setFamilyName("O'BRIEN");
        a.setDob(DOB);
        return a;
    }

    private static PassportRequest passport(String given, String family, LocalDate dob, LocalDate expiry) {
        return new PassportRequest("p1234567", given, family, dob, LocalDate.now().minusYears(2), expiry, "MRZ", false);
    }

    private void studentCanEdit(Applicant applicant) {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(caller.requireUserId()).thenReturn(1L);
        when(caller.isExternal()).thenReturn(true);
        when(mapper.findById(ID)).thenReturn(applicant);
    }

    @Test
    void shouldSaveNormalisedPassportData_andReportAMatch() {
        studentCanEdit(profile());

        PassportStatusResponse status = service.save(ID,
                passport("ahmed", "obrien", DOB, LocalDate.now().plusYears(3)));

        ArgumentCaptor<Applicant> saved = ArgumentCaptor.forClass(Applicant.class);
        verify(mapper).updatePassportData(saved.capture());
        assertThat(saved.getValue().getPassportNo()).isEqualTo("P1234567");
        assertThat(saved.getValue().getPassportGivenName()).isEqualTo("AHMED");
        assertThat(saved.getValue().getPassportReadMethod()).isEqualTo("MRZ");
        assertThat(status.matchesProfile()).isTrue();
        assertThat(status.validForAdmission()).isTrue();
    }

    @Test
    void shouldStillSave_butReportTheDifferingFields_whenPassportAndProfileDisagree() {
        studentCanEdit(profile());

        PassportStatusResponse status = service.save(ID,
                passport("ahmad", "obrien", DOB.plusDays(1), LocalDate.now().plusYears(3)));

        verify(mapper).updatePassportData(any());
        assertThat(status.matchesProfile()).isFalse();
        assertThat(status.mismatches()).extracting(PassportStatusResponse.Mismatch::field)
                .containsExactly("givenName", "dob");
    }

    @Test
    void shouldRejectAndNotSave_whenThePassportExpiresWithinSixMonths() {
        studentCanEdit(profile());

        assertThatThrownBy(() -> service.save(ID, passport("ahmed", "obrien", DOB, LocalDate.now().plusMonths(3))))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("six months");
        verify(mapper, never()).updatePassportData(any());
    }

    @Test
    void shouldRefuse_whenCallerCannotEditThisApplicant() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.save(ID, passport("ahmed", "obrien", DOB, LocalDate.now().plusYears(3))))
                .isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).updatePassportData(any());
    }

    @Test
    void shouldMaskPassportPii_forStaffWithoutThePiiPermission() {
        Applicant a = profile();
        a.setPassportNo("P1234567");
        a.setPassportGivenName("AHMED");
        a.setPassportFamilyName("OBRIEN");
        a.setPassportDob(DOB);
        when(access.canAccessApplicant(ID, "VIEW_PROFILE")).thenReturn(true);
        when(caller.isExternal()).thenReturn(false);
        when(rbac.hasPermi("nad:applicant:pii:view")).thenReturn(false);
        when(mapper.findById(ID)).thenReturn(a);

        PassportStatusResponse status = service.status(ID);

        assertThat(status.passportNo()).isEqualTo("••••");
        assertThat(status.dob()).isEqualTo("••••");
    }
}
