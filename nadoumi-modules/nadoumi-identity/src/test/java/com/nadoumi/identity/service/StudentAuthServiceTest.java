package com.nadoumi.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadForbiddenException;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.TicketService;
import com.nadoumi.identity.web.request.StudentLoginRequest;
import com.nadoumi.identity.web.request.StudentRegisterRequest;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentAuthServiceTest {

    private final ISysConfigService configService = mock(ISysConfigService.class);
    private final ISysUserService userService = mock(ISysUserService.class);
    private final SysLoginService loginService = mock(SysLoginService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final NadIdentityMapper identityMapper = mock(NadIdentityMapper.class);
    private final UserApplicantAccessMapper accessMapper = mock(UserApplicantAccessMapper.class);
    private final UserApplicantAccessService grants = mock(UserApplicantAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final TicketService tickets = mock(TicketService.class);
    private final SessionRevoker sessionRevoker = mock(SessionRevoker.class);
    private final org.springframework.context.ApplicationEventPublisher events =
            mock(org.springframework.context.ApplicationEventPublisher.class);
    private final com.nadoumi.common.outbox.OutboxWriter outbox =
            mock(com.nadoumi.common.outbox.OutboxWriter.class);

    private final StudentAuthService service = new StudentAuthService(configService, userService, loginService,
            tokenService, identityMapper, accessMapper, grants, caller, tickets, sessionRevoker, events, outbox);

    private static StudentRegisterRequest register(String email, String password, String ticket) {
        return new StudentRegisterRequest("Ada", "Lovelace", email, password, ticket);
    }

    @BeforeEach
    void registrationEnabled() {
        when(configService.selectConfigByKey(StudentAuthService.REGISTER_ENABLED_KEY)).thenReturn("true");
    }

    @Test
    void register_is_forbidden_when_the_feature_flag_is_off() {
        when(configService.selectConfigByKey(StudentAuthService.REGISTER_ENABLED_KEY)).thenReturn("false");
        assertThatThrownBy(() -> service.register(register("a@x.com", "Abcdef1!", "tkt")))
                .isInstanceOf(NadForbiddenException.class);
    }

    @Test
    void register_rejects_a_ticket_that_was_verified_for_a_different_email() {
        when(tickets.consume("tkt", OtpPurpose.REGISTER)).thenReturn("someone-else@x.com");
        assertThatThrownBy(() -> service.register(register("a@x.com", "Abcdef1!", "tkt")))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void register_rejects_a_password_that_fails_the_policy() {
        when(tickets.consume("tkt", OtpPurpose.REGISTER)).thenReturn("a@x.com");
        assertThatThrownBy(() -> service.register(register("a@x.com", "weak", "tkt")))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("password.tooShort");
    }

    @Test
    void register_rejects_a_duplicate_email() {
        when(tickets.consume("tkt", OtpPurpose.REGISTER)).thenReturn("a@x.com");
        when(identityMapper.selectUserIdByEmailAndType("a@x.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(5L);
        assertThatThrownBy(() -> service.register(register("a@x.com", "Abcdef1!", "tkt")))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_creates_the_user_generates_a_handle_and_marks_the_email_verified() {
        when(tickets.consume("tkt", OtpPurpose.REGISTER)).thenReturn("ada@example.com");
        when(identityMapper.selectUserIdByEmailAndType("ada@example.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(null);
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.registerUser(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setUserId(42L);
            return true;
        });

        var response = service.register(register("Ada@Example.com", "Abcdef1!", "tkt"));

        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.username()).startsWith("stu_");
        verify(identityMapper).updateUserType(42L, StudentAuthService.STUDENT_USER_TYPE);
        verify(identityMapper).markEmailVerified(42L);

        org.mockito.ArgumentCaptor<String> payload = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(outbox).write(eq("user"), eq(42L),
                eq(com.nadoumi.common.outbox.OutboxEventTypes.STUDENT_REGISTERED), payload.capture());
        assertThat(payload.getValue()).contains("\"userId\":42").contains("ada@example.com").contains("Ada");
    }

    @Test
    void resetPassword_publishes_a_password_changed_event() {
        when(tickets.consume("tkt", OtpPurpose.PASSWORD_RESET)).thenReturn("ada@example.com");
        when(identityMapper.selectUserIdByEmailAndType("ada@example.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(42L);

        service.resetPassword("tkt", "Abcdef1!");

        org.mockito.ArgumentCaptor<com.nadoumi.identity.service.mail.PasswordChangedEvent> ev =
                org.mockito.ArgumentCaptor.forClass(com.nadoumi.identity.service.mail.PasswordChangedEvent.class);
        verify(events).publishEvent(ev.capture());
        assertThat(ev.getValue().userId()).isEqualTo(42L);
        assertThat(ev.getValue().email()).isEqualTo("ada@example.com");
    }

    @Test
    void login_with_an_unknown_email_is_the_same_error_as_a_wrong_password() {
        when(identityMapper.selectUserIdByEmailAndType("a@x.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(null);
        assertThatThrownBy(() -> service.login(new StudentLoginRequest("a@x.com", "pw", null, null)))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("email or password is incorrect");
    }

    @Test
    void login_resolves_the_handle_delegates_to_SysLoginService_and_accepts_invites() {
        when(identityMapper.selectUserIdByEmailAndType("a@x.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(7L);
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUserName("stu_ax");
        user.setEmail("a@x.com");
        when(userService.selectUserById(7L)).thenReturn(user);
        when(loginService.login("stu_ax", "pw", "c", "u")).thenReturn("jwt-token");

        String token = service.login(new StudentLoginRequest("a@x.com", "pw", "c", "u"));

        assertThat(token).isEqualTo("jwt-token");
        verify(grants).acceptInvitesFor(eq(7L), eq("a@x.com"));
    }

    @Test
    void studentEmailExists_delegates_to_the_type_scoped_lookup() {
        when(identityMapper.selectUserIdByEmailAndType("a@x.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(1L);
        assertThat(service.studentEmailExists("A@X.com")).isTrue();
    }

    @Test
    void resetPassword_applies_the_new_password_revokes_every_session_and_issues_no_token() {
        when(tickets.consume("tkt", OtpPurpose.PASSWORD_RESET)).thenReturn("a@x.com");
        when(identityMapper.selectUserIdByEmailAndType("a@x.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(7L);

        service.resetPassword("tkt", "BrandNew1!");

        verify(userService).resetUserPwd(eq(7L), any(String.class));
        verify(identityMapper).touchPwdUpdateDate(7L);
        verify(sessionRevoker).revokeAll(7L, null);
    }

    @Test
    void resetPassword_rejects_a_weak_password() {
        when(tickets.consume("tkt", OtpPurpose.PASSWORD_RESET)).thenReturn("a@x.com");
        when(identityMapper.selectUserIdByEmailAndType("a@x.com", StudentAuthService.STUDENT_USER_TYPE))
                .thenReturn(7L);

        assertThatThrownBy(() -> service.resetPassword("tkt", "weak"))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("password.tooShort");
    }

    @Test
    void changePassword_verifies_the_current_password_and_keeps_the_callers_session() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String currentEncoded = SecurityUtils.encryptPassword("OldPass1!");
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setPassword(currentEncoded);
        when(caller.requireUserId()).thenReturn(7L);
        when(userService.selectUserById(7L)).thenReturn(user);
        LoginUser me = new LoginUser();
        me.setUser(user);
        me.setToken("my-token");
        when(tokenService.getLoginUser(request)).thenReturn(me);

        service.changePassword(request, "OldPass1!", "NewPass1!");

        verify(userService).resetUserPwd(eq(7L), any(String.class));
        verify(sessionRevoker).revokeAll(7L, "my-token");
        verify(tokenService).setLoginUser(me);
    }

    @Test
    void changePassword_rejects_a_wrong_current_password() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setPassword(SecurityUtils.encryptPassword("OldPass1!"));
        when(caller.requireUserId()).thenReturn(7L);
        when(userService.selectUserById(7L)).thenReturn(user);

        assertThatThrownBy(() -> service.changePassword(request, "WrongPass1!", "NewPass1!"))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("current password is incorrect");
    }
}
