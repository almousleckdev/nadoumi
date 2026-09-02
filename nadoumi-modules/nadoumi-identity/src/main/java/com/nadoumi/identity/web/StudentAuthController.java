package com.nadoumi.identity.web;

import com.nadoumi.identity.service.StudentAuthService;
import com.nadoumi.identity.web.request.StudentLoginRequest;
import com.nadoumi.identity.web.request.StudentRegisterRequest;
import com.nadoumi.identity.web.response.StudentIdentityResponse;
import com.nadoumi.identity.web.response.StudentRegisterResponse;
import com.nadoumi.identity.web.response.TokenResponse;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentAuthController {

    private final StudentAuthService service;

    public StudentAuthController(StudentAuthService service) {
        this.service = service;
    }

    @Anonymous
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimiter(count = 5, time = 60)
    public StudentRegisterResponse register(@Valid @RequestBody StudentRegisterRequest req) {
        return service.register(req);
    }

    @Anonymous
    @PostMapping("/login")
    @RateLimiter(count = 10, time = 60)
    public TokenResponse login(@Valid @RequestBody StudentLoginRequest req) {
        return new TokenResponse(service.login(req));
    }

    @GetMapping("/me")
    public StudentIdentityResponse me() {
        return service.me();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        service.logout(request);
    }
}
