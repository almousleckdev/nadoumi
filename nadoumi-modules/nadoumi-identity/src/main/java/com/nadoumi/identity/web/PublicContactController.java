package com.nadoumi.identity.web;

import com.nadoumi.identity.contact.ContactService;
import com.nadoumi.identity.web.request.ContactRequest;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anonymous "Contact us" endpoint for the public website. IP rate-limited.
 * Always answers {@code 202} — a bot submission (honeypot) is dropped silently
 * rather than signalled.
 */
@RestController
public class PublicContactController {

    private final ContactService service;

    public PublicContactController(ContactService service) {
        this.service = service;
    }

    @Anonymous
    @PostMapping("/api/public/contact")
    @RateLimiter(time = 3600, count = 10, limitType = LimitType.IP)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void submit(@Valid @RequestBody ContactRequest req, HttpServletRequest http) {
        service.submit(req, IpUtils.getIpAddr(http), http.getHeader("User-Agent"));
    }
}
