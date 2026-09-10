package com.nadoumi.identity.service;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.access.AccessCapabilityMatrix;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.access.CapabilityOverrides;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadForbiddenException;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.nadoumi.identity.service.mail.PasswordChangedEvent;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.TicketService;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.nadoumi.identity.web.request.StudentLoginRequest;
import com.nadoumi.identity.web.request.StudentRegisterRequest;
import com.nadoumi.identity.web.response.AccessibleApplicant;
import com.nadoumi.identity.web.response.StudentIdentityResponse;
import com.nadoumi.identity.web.response.StudentRegisterResponse;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PasswordPolicy;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * External-user (student / agent / guardian) authentication. Reuses the RuoYi login
 * pipeline (captcha, lockout, JWT, Redis session) but keeps externals off the staff
 * endpoints and gives them a roles-free identity payload.
 *
 * <p>Revision 2: identity is email-first. Registration requires an OTP {@code ticket}
 * proving the email was verified; {@code user_name} is generated server-side; login
 * resolves the user by verified email.
 */
@Service
public class StudentAuthService {

    static final String REGISTER_ENABLED_KEY = "nad.student.register.enabled";
    static final String STUDENT_USER_TYPE = "10";

    private final ISysConfigService configService;
    private final ISysUserService userService;
    private final SysLoginService loginService;
    private final TokenService tokenService;
    private final NadIdentityMapper identityMapper;
    private final UserApplicantAccessMapper accessMapper;
    private final UserApplicantAccessService grants;
    private final CurrentCaller caller;
    private final TicketService tickets;
    private final SessionRevoker sessionRevoker;
    private final ApplicationEventPublisher events;
    private final OutboxWriter outbox;

    public StudentAuthService(ISysConfigService configService, ISysUserService userService,
            SysLoginService loginService, TokenService tokenService, NadIdentityMapper identityMapper,
            UserApplicantAccessMapper accessMapper, UserApplicantAccessService grants, CurrentCaller caller,
            TicketService tickets, SessionRevoker sessionRevoker, ApplicationEventPublisher events,
            OutboxWriter outbox) {
        this.configService = configService;
        this.userService = userService;
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.identityMapper = identityMapper;
        this.accessMapper = accessMapper;
        this.grants = grants;
        this.caller = caller;
        this.tickets = tickets;
        this.sessionRevoker = sessionRevoker;
        this.events = events;
        this.outbox = outbox;
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentRegisterResponse register(StudentRegisterRequest req) {
        if (!"true".equalsIgnoreCase(configService.selectConfigByKey(REGISTER_ENABLED_KEY))) {
            throw new NadForbiddenException("student registration is disabled");
        }
        String email = normalizeEmail(req.email());
        String verifiedEmail = tickets.consume(req.ticket(), OtpPurpose.REGISTER);
        if (!verifiedEmail.equals(email)) {
            throw new NadBadRequestException("email verification does not match this address");
        }
        PasswordPolicy.violation(req.password(), null,
                        List.of(req.firstName(), req.lastName(), emailLocalPart(email)))
                .ifPresent(key -> { throw new NadBadRequestException(key); });
        if (identityMapper.selectUserIdByEmailAndType(email, STUDENT_USER_TYPE) != null) {
            throw new NadBadRequestException("email already registered");
        }

        SysUser user = new SysUser();
        user.setUserName(generateStudentHandle(email));
        user.setNickName(req.firstName().trim() + " " + req.lastName().trim());
        user.setEmail(email);
        user.setPassword(SecurityUtils.encryptPassword(req.password()));
        user.setPwdUpdateDate(DateUtils.getNowDate());
        if (!userService.registerUser(user)) {
            throw new NadBadRequestException("registration failed");
        }
        identityMapper.updateUserType(user.getUserId(), STUDENT_USER_TYPE);
        identityMapper.markEmailVerified(user.getUserId());

        // Committed with the account row; the poller fans it to the Welcome email.
        JSONObject payload = new JSONObject();
        payload.put("userId", user.getUserId());
        payload.put("email", email);
        payload.put("firstName", req.firstName().trim());
        payload.put("displayName", user.getNickName());
        payload.put("locale", "en");
        outbox.write("user", user.getUserId(), OutboxEventTypes.STUDENT_REGISTERED, payload.toJSONString());

        return new StudentRegisterResponse(user.getUserId(), user.getUserName());
    }

    @Transactional(rollbackFor = Exception.class)
    public String login(StudentLoginRequest req) {
        String email = normalizeEmail(req.email());
        Long userId = identityMapper.selectUserIdByEmailAndType(email, STUDENT_USER_TYPE);
        if (userId == null) {
            // never disclose whether the address is registered
            throw new NadBadRequestException("email or password is incorrect");
        }
        SysUser user = userService.selectUserById(userId);
        String token = loginService.login(user.getUserName(), req.password(), req.code(), req.uuid());
        grants.acceptInvitesFor(userId, user.getEmail());
        return token;
    }

    public boolean studentEmailExists(String email) {
        return identityMapper.selectUserIdByEmailAndType(normalizeEmail(email), STUDENT_USER_TYPE) != null;
    }

    /**
     * Complete a forgotten-password reset. Consumes the ticket, applies the policy,
     * sets the new password, and revokes <em>every</em> session for the account.
     * Issues no token and performs no login (spec §15.4).
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String ticket, String newPassword) {
        String email = tickets.consume(ticket, OtpPurpose.PASSWORD_RESET);
        Long userId = identityMapper.selectUserIdByEmailAndType(email, STUDENT_USER_TYPE);
        if (userId == null) {
            // ticket was valid but the account is gone - nothing to do, reveal nothing
            return;
        }
        PasswordPolicy.violation(newPassword, null, List.of(emailLocalPart(email)))
                .ifPresent(key -> { throw new NadBadRequestException(key); });
        userService.resetUserPwd(userId, SecurityUtils.encryptPassword(newPassword));
        identityMapper.touchPwdUpdateDate(userId);
        sessionRevoker.revokeAll(userId, null);
        events.publishEvent(new PasswordChangedEvent(userId, email));
    }

    /**
     * Signed-in student changing their own password: verify the current one, apply
     * the policy (including "different from current"), then revoke every session
     * except the caller's and refresh the caller's cached credentials.
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(HttpServletRequest request, String currentPassword, String newPassword) {
        Long userId = caller.requireUserId();
        SysUser user = userService.selectUserById(userId);
        if (!SecurityUtils.matchesPassword(currentPassword, user.getPassword())) {
            throw new NadBadRequestException("current password is incorrect");
        }
        PasswordPolicy.violation(newPassword, user.getPassword(),
                        personalTerms(user.getNickName(), user.getEmail()))
                .ifPresent(key -> { throw new NadBadRequestException(key); });

        String encoded = SecurityUtils.encryptPassword(newPassword);
        userService.resetUserPwd(userId, encoded);
        identityMapper.touchPwdUpdateDate(userId);

        LoginUser me = tokenService.getLoginUser(request);
        sessionRevoker.revokeAll(userId, me != null ? me.getToken() : null);
        if (me != null) {
            me.getUser().setPassword(encoded);
            tokenService.setLoginUser(me);
        }
        events.publishEvent(new PasswordChangedEvent(userId, user.getEmail()));
    }

    public StudentIdentityResponse me() {
        Long userId = caller.requireUserId();
        if (!caller.isExternal()) {
            throw new NadForbiddenException("staff identity is served by /getInfo");
        }
        SysUser user = userService.selectUserById(userId);
        List<AccessibleApplicant> accessible = accessMapper.findActiveGrantsForUser(userId).stream()
                .map(StudentAuthService::toAccessible)
                .toList();
        return new StudentIdentityResponse(userId, user.getUserName(), user.getNickName(), user.getEmail(), accessible);
    }

    public void logout(HttpServletRequest request) {
        var loginUser = tokenService.getLoginUser(request);
        if (loginUser != null) {
            tokenService.delLoginUser(loginUser.getToken());
        }
    }

    /**
     * A unique, opaque internal {@code user_name} for a student. Never shown to the
     * user; the verified email and the display name carry identity.
     */
    private String generateStudentHandle(String email) {
        String base = "stu_" + email.replaceAll("[^a-z0-9]", "");
        if (base.length() > 18) {
            base = base.substring(0, 18);
        }
        SysUser probe = new SysUser();
        for (int suffix = 0; ; suffix++) {
            String candidate = suffix == 0 ? base : base + suffix;
            probe.setUserName(candidate);
            if (userService.checkUserNameUnique(probe)) {
                return candidate;
            }
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String emailLocalPart(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    /** Personal terms a password must not contain: name words + email local-part. */
    private static List<String> personalTerms(String displayName, String email) {
        List<String> terms = new ArrayList<>();
        if (displayName != null) {
            for (String word : displayName.trim().split("\\s+")) {
                if (!word.isBlank()) terms.add(word);
            }
        }
        if (email != null && !email.isBlank()) {
            terms.add(emailLocalPart(email));
        }
        return terms;
    }

    private static AccessibleApplicant toAccessible(UserApplicantAccess g) {
        CapabilityOverrides ov = CapabilityOverrides.parse(g.getCapabilityOverridesJson());
        List<String> caps = AccessCapabilityMatrix.effectiveCapabilities(g.getAccessRole(), ov.add(), ov.remove())
                .stream().map(Enum::name).sorted().toList();
        return new AccessibleApplicant(g.getApplicantId(), g.getAccessRole().name(), caps);
    }
}
