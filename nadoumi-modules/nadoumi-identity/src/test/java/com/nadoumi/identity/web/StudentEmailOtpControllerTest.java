package com.nadoumi.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.service.StudentAuthService;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.OtpService;
import com.nadoumi.identity.service.otp.OtpService.IssueResult;
import com.nadoumi.identity.web.request.EmailOtpRequest;
import com.nadoumi.identity.web.request.EmailOtpVerifyRequest;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.system.service.ISysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentEmailOtpControllerTest {

    private final OtpService otp = mock(OtpService.class);
    private final StudentAuthService auth = mock(StudentAuthService.class);
    private final ISysConfigService config = mock(ISysConfigService.class);
    private final SysLoginService login = mock(SysLoginService.class);
    private final StudentEmailOtpController controller = new StudentEmailOtpController(otp, auth, config, login);

    @BeforeEach
    void freshCodeByDefault() {
        lenient().when(otp.issue(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), anyBoolean())).thenReturn(new IssueResult(true, 60));
    }

    @Test
    void an_unknown_register_email_gets_an_otp() {
        when(auth.studentEmailExists("a@x.com")).thenReturn(false);

        var res = controller.request(new EmailOtpRequest("a@x.com", OtpPurpose.REGISTER, null, null));

        assertThat(res.sent()).isTrue();
        assertThat(res.throttled()).isFalse();
        verify(otp).issue("a@x.com", OtpPurpose.REGISTER, false);
    }

    @Test
    void an_already_registered_email_is_handled_by_the_account_exists_branch() {
        when(auth.studentEmailExists("a@x.com")).thenReturn(true);

        assertThat(controller.request(new EmailOtpRequest("a@x.com", OtpPurpose.REGISTER, null, null)).sent()).isTrue();
        verify(otp).issue("a@x.com", OtpPurpose.REGISTER, true);
    }

    @Test
    void a_password_reset_request_always_issues_an_otp() {
        controller.request(new EmailOtpRequest("a@x.com", OtpPurpose.PASSWORD_RESET, null, null));
        verify(otp).issue("a@x.com", OtpPurpose.PASSWORD_RESET, false);
    }

    @Test
    void a_throttled_issue_is_reported_with_retry_after_and_still_sent_true() {
        when(auth.studentEmailExists("a@x.com")).thenReturn(false);
        when(otp.issue("a@x.com", OtpPurpose.REGISTER, false)).thenReturn(new IssueResult(false, 25));

        var res = controller.request(new EmailOtpRequest("a@x.com", OtpPurpose.REGISTER, null, null));

        assertThat(res.sent()).isTrue();
        assertThat(res.throttled()).isTrue();
        assertThat(res.retryAfter()).isEqualTo(25);
    }

    @Test
    void captcha_is_validated_when_enabled() {
        when(config.selectCaptchaEnabled()).thenReturn(true);

        controller.request(new EmailOtpRequest("a@x.com", OtpPurpose.PASSWORD_RESET, "1234", "uuid-1"));

        verify(login).validateCaptcha("a@x.com", "1234", "uuid-1");
    }

    @Test
    void verify_returns_the_ticket_from_the_otp_service() {
        when(otp.verify("a@x.com", OtpPurpose.REGISTER, "482913")).thenReturn("tkt_9");

        assertThat(controller.verify(new EmailOtpVerifyRequest("a@x.com", OtpPurpose.REGISTER, "482913")).ticket())
                .isEqualTo("tkt_9");
    }
}
