package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.identity.service.otp.OtpException;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ApplicantEmailServiceTest {

    private static final long ID = 7L;

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final OtpService otp = mock(OtpService.class);
    private final ApplicantEmailService service = new ApplicantEmailService(mapper, new ApplicantAccessGuard(access), otp);

    @Test
    void shouldSendACode_toTheNormalisedNewAddress() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(otp.issue("new@example.com", OtpPurpose.APPLICANT_EMAIL, false))
                .thenReturn(new OtpService.IssueResult(true, 60));

        OtpService.IssueResult result = service.requestCode(ID, "  New@Example.com ");

        assertThat(result.sent()).isTrue();
    }

    @Test
    void shouldStoreAndVerifyTheEmail_whenTheCodeIsRight() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(otp.verify("new@example.com", OtpPurpose.APPLICANT_EMAIL, "123456")).thenReturn("ticket");
        Applicant updated = new Applicant();
        updated.setId(ID);
        when(mapper.findById(ID)).thenReturn(updated);

        service.verify(ID, "new@example.com", "123456");

        verify(mapper).markEmailVerified(ID, "new@example.com");
    }

    @Test
    void shouldNotStoreTheEmail_whenTheCodeIsWrong() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(otp.verify("new@example.com", OtpPurpose.APPLICANT_EMAIL, "000000"))
                .thenThrow(new OtpException("verification code is invalid or expired"));

        assertThatThrownBy(() -> service.verify(ID, "new@example.com", "000000")).isInstanceOf(OtpException.class);
        verify(mapper, never()).markEmailVerified(ID, "new@example.com");
    }

    @Test
    void shouldRefuse_whenCallerCannotEditThisApplicant() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.requestCode(ID, "new@example.com")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.verify(ID, "new@example.com", "123456")).isInstanceOf(AccessDeniedException.class);
    }
}
