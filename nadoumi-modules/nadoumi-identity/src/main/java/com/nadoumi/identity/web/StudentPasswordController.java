package com.nadoumi.identity.web;

import com.nadoumi.identity.service.StudentAuthService;
import com.nadoumi.identity.web.request.ChangePasswordRequest;
import com.nadoumi.identity.web.request.PasswordResetRequest;
import com.ruoyi.common.annotation.Anonymous;
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
 * Student password reset (anonymous, ticket-gated) and change (bearer). Both revoke
 * other sessions; neither returns a token.
 */
@RestController
@RequestMapping("/api/student/password")
public class StudentPasswordController {

    private final StudentAuthService service;

    public StudentPasswordController(StudentAuthService service) {
        this.service = service;
    }

    @Anonymous
    @PostMapping("/reset")
    @RateLimiter(time = 600, count = 10, limitType = LimitType.IP)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody PasswordResetRequest req) {
        service.resetPassword(req.ticket(), req.newPassword());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest req) {
        service.changePassword(request, req.currentPassword(), req.newPassword());
    }
}
