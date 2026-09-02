package com.nadoumi.identity.web;

import com.nadoumi.identity.service.StudentAuthService;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.OtpService;
import com.nadoumi.identity.web.request.EmailOtpRequest;
import com.nadoumi.identity.web.request.EmailOtpVerifyRequest;
import com.nadoumi.identity.web.response.OtpSentResponse;
import com.nadoumi.identity.web.response.TicketResponse;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.system.service.ISysConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Email one-time codes for {@code REGISTER} and {@code PASSWORD_RESET}. Both
 * endpoints are anonymous, IP rate-limited and captcha-aware. {@code /email-otp}
 * never reveals whether an address is registered.
 */
@RestController
@RequestMapping("/api/student/email-otp")
public class StudentEmailOtpController {

    private final OtpService otp;
    private final StudentAuthService auth;
    private final ISysConfigService configService;
    private final SysLoginService loginService;

    public StudentEmailOtpController(OtpService otp, StudentAuthService auth,
            ISysConfigService configService, SysLoginService loginService) {
        this.otp = otp;
        this.auth = auth;
        this.configService = configService;
        this.loginService = loginService;
    }

    @Anonymous
    @PostMapping
    @RateLimiter(time = 3600, count = 20, limitType = LimitType.IP)
    public OtpSentResponse request(@Valid @RequestBody EmailOtpRequest req) {
        if (configService.selectCaptchaEnabled()) {
            loginService.validateCaptcha(req.email(), req.code(), req.uuid());
        }
        if (req.purpose() == OtpPurpose.REGISTER && auth.studentEmailExists(req.email())) {
            otp.sendAccountExists(req.email());
        }
        else {
            otp.issue(req.email(), req.purpose());
        }
        return new OtpSentResponse(true);
    }

    @Anonymous
    @PostMapping("/verify")
    @RateLimiter(time = 600, count = 10, limitType = LimitType.IP)
    public TicketResponse verify(@Valid @RequestBody EmailOtpVerifyRequest req) {
        return new TicketResponse(otp.verify(req.email(), req.purpose(), req.otp()));
    }
}
