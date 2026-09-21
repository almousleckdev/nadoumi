package com.nadoumi.identity.web;

import com.nadoumi.identity.service.StudentAuthService;
import com.nadoumi.identity.service.otp.OtpService;
import com.nadoumi.identity.web.request.EmailChangeCodeRequest;
import com.nadoumi.identity.web.request.EmailChangeRequest;
import com.nadoumi.identity.web.response.OtpSentResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signed-in student changing their own sign-in email. Both endpoints require a
 * bearer token — self-service email change is never anonymous, unlike password
 * reset. The new address must be proven with an OTP before it is written.
 */
@RestController
@RequestMapping("/api/student/email/change")
public class StudentEmailChangeController {

    private final StudentAuthService service;

    public StudentEmailChangeController(StudentAuthService service) {
        this.service = service;
    }

    @PostMapping("/otp")
    @RateLimiter(time = 3600, count = 10, limitType = LimitType.IP)
    public OtpSentResponse requestCode(@Valid @RequestBody EmailChangeCodeRequest req) {
        OtpService.IssueResult result = service.requestEmailChangeCode(req.newEmail());
        return new OtpSentResponse(true, !result.sent(), result.retryAfterSeconds());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RateLimiter(time = 600, count = 10, limitType = LimitType.IP)
    public void change(HttpServletRequest request, @Valid @RequestBody EmailChangeRequest req) {
        service.changeEmail(request, req.newEmail(), req.otp(), req.currentPassword());
    }
}
