package com.nadoumi.identity.service;

import com.nadoumi.common.access.AccessCapabilityMatrix;
import com.nadoumi.identity.access.CapabilityOverrides;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadForbiddenException;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.nadoumi.identity.web.request.StudentLoginRequest;
import com.nadoumi.identity.web.request.StudentRegisterRequest;
import com.nadoumi.identity.web.response.AccessibleApplicant;
import com.nadoumi.identity.web.response.StudentIdentityResponse;
import com.nadoumi.identity.web.response.StudentRegisterResponse;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * External-user (student / agent / guardian) authentication. Reuses the RuoYi login
 * pipeline (captcha, lockout, JWT, Redis session) but keeps externals off the staff
 * endpoints and gives them a roles-free identity payload.
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

    public StudentAuthService(ISysConfigService configService, ISysUserService userService,
            SysLoginService loginService, TokenService tokenService, NadIdentityMapper identityMapper,
            UserApplicantAccessMapper accessMapper, UserApplicantAccessService grants, CurrentCaller caller) {
        this.configService = configService;
        this.userService = userService;
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.identityMapper = identityMapper;
        this.accessMapper = accessMapper;
        this.grants = grants;
        this.caller = caller;
    }

    @Transactional
    public StudentRegisterResponse register(StudentRegisterRequest req) {
        if (!"true".equalsIgnoreCase(configService.selectConfigByKey(REGISTER_ENABLED_KEY))) {
            throw new NadForbiddenException("student registration is disabled");
        }
        if (configService.selectCaptchaEnabled()) {
            loginService.validateCaptcha(req.username(), req.code(), req.uuid());
        }
        SysUser probe = new SysUser();
        probe.setUserName(req.username());
        if (!userService.checkUserNameUnique(probe)) {
            throw new NadBadRequestException("username already registered");
        }
        SysUser user = new SysUser();
        user.setUserName(req.username());
        user.setNickName(StringUtils.isEmpty(req.nickName()) ? req.username() : req.nickName());
        user.setEmail(req.email());
        user.setPassword(SecurityUtils.encryptPassword(req.password()));
        user.setPwdUpdateDate(DateUtils.getNowDate());
        if (!userService.registerUser(user)) {
            throw new NadBadRequestException("registration failed");
        }
        identityMapper.updateUserType(user.getUserId(), STUDENT_USER_TYPE);
        return new StudentRegisterResponse(user.getUserId(), user.getUserName());
    }

    @Transactional
    public String login(StudentLoginRequest req) {
        Long userId = identityMapper.selectUserIdByUserName(req.username());
        if (userId != null && CurrentCaller.STAFF.equals(nvl(identityMapper.selectUserType(userId)))) {
            throw new NadForbiddenException("staff must sign in at /login");
        }
        String token = loginService.login(req.username(), req.password(), req.code(), req.uuid());
        Long resolved = userId != null ? userId : identityMapper.selectUserIdByUserName(req.username());
        if (resolved != null) {
            SysUser user = userService.selectUserById(resolved);
            grants.acceptInvitesFor(resolved, user != null ? user.getEmail() : null);
        }
        return token;
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
        return new StudentIdentityResponse(userId, user.getUserName(), user.getNickName(), accessible);
    }

    public void logout(HttpServletRequest request) {
        var loginUser = tokenService.getLoginUser(request);
        if (loginUser != null) {
            tokenService.delLoginUser(loginUser.getToken());
        }
    }

    private static AccessibleApplicant toAccessible(UserApplicantAccess g) {
        CapabilityOverrides ov = CapabilityOverrides.parse(g.getCapabilityOverridesJson());
        List<String> caps = AccessCapabilityMatrix.effectiveCapabilities(g.getAccessRole(), ov.add(), ov.remove())
                .stream().map(Enum::name).sorted().toList();
        return new AccessibleApplicant(g.getApplicantId(), g.getAccessRole().name(), caps);
    }

    private static String nvl(String userType) {
        return userType == null ? CurrentCaller.STAFF : userType;
    }
}
