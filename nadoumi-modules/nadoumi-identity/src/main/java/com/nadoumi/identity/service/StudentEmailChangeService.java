package com.nadoumi.identity.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.service.mail.EmailChangedEvent;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.OtpService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A signed-in student changing their sign-in email, proven by a code mailed to the new address. */
@Service
public class StudentEmailChangeService {

    private final NadIdentityMapper identityMapper;
    private final ISysUserService userService;
    private final TokenService tokenService;
    private final CurrentCaller caller;
    private final OtpService otp;
    private final ApplicationEventPublisher events;

    public StudentEmailChangeService(NadIdentityMapper identityMapper, ISysUserService userService,
            TokenService tokenService, CurrentCaller caller, OtpService otp, ApplicationEventPublisher events) {
        this.identityMapper = identityMapper;
        this.userService = userService;
        this.tokenService = tokenService;
        this.caller = caller;
        this.otp = otp;
        this.events = events;
    }

    /** A code is mailed to the new address, proving they control it, before anything changes. */
    public OtpService.IssueResult requestCode(String newEmail) {
        caller.requireUserId();
        String email = StudentEmails.normalize(newEmail);
        requireUnused(email);
        return otp.issue(email, OtpPurpose.EMAIL_CHANGE, false);
    }

    /**
     * Applies the change: verifies the current password (defense against a hijacked session, since
     * email is the sign-in identity) and the OTP proving ownership of the new address, then updates
     * the account and notifies the <em>previous</em> address in case this was not the account holder.
     */
    @Transactional(rollbackFor = Exception.class)
    public void change(HttpServletRequest request, String newEmail, String otpCode, String currentPassword) {
        Long userId = caller.requireUserId();
        SysUser user = userService.selectUserById(userId);
        if (!SecurityUtils.matchesPassword(currentPassword, user.getPassword())) {
            throw new NadBadRequestException("current password is incorrect");
        }
        String email = StudentEmails.normalize(newEmail);
        String previousEmail = user.getEmail();
        if (email.equals(StudentEmails.normalize(previousEmail == null ? "" : previousEmail))) {
            throw new NadBadRequestException("that is already your sign-in email");
        }
        otp.verify(email, OtpPurpose.EMAIL_CHANGE, otpCode);
        requireUnused(email);
        identityMapper.changeEmail(userId, email);

        LoginUser me = tokenService.getLoginUser(request);
        if (me != null) {
            me.getUser().setEmail(email);
            tokenService.setLoginUser(me);
        }
        events.publishEvent(new EmailChangedEvent(userId, previousEmail, email));
    }

    /** The OTP proves ownership, but another account could have claimed the address meanwhile. */
    private void requireUnused(String email) {
        if (identityMapper.selectUserIdByEmailAndType(email, StudentAuthService.STUDENT_USER_TYPE) != null) {
            throw new NadBadRequestException("that email is already in use");
        }
    }
}
