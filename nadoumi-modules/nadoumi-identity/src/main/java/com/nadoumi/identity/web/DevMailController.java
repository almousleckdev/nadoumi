package com.nadoumi.identity.web;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.LoggingMailSender;
import com.ruoyi.common.annotation.Anonymous;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Non-production helper: returns the last email captured for a recipient, so the
 * Playwright {@code readOtp} helper (and manual dev testing) can read an OTP
 * without a real mailbox.
 *
 * <p>This endpoint is {@link Anonymous} and returns raw OTP / password-reset codes,
 * so it must never be reachable in production. It is mounted only when both hold:
 * <ul>
 *   <li>{@code nadoumi.mail.dev-inbox.enabled=true} — an explicit opt-in, absent
 *       (and therefore off) by default and in every committed config. It is
 *       deliberately separate from {@code nadoumi.mail.transport}: choosing the
 *       no-network {@link LoggingMailSender} must not by itself expose codes over
 *       HTTP.</li>
 *   <li>the active profiles do not include {@code prod} — declarative
 *       defence-in-depth for the planned {@code application-prod.yml}.</li>
 * </ul>
 * The flag is set in {@code src/test/resources/application-test.yml} for the
 * integration tests; local E2E runs set it in {@code config/application-local.yml}.
 * Requires {@code nadoumi.mail.transport=log} (the default) so that
 * {@link LoggingMailSender} is the active {@code MailSender}.
 */
@RestController
@RequestMapping("/api/dev/mail")
@Profile("!prod")
@ConditionalOnProperty(name = "nadoumi.mail.dev-inbox.enabled", havingValue = "true", matchIfMissing = false)
public class DevMailController {

    private final LoggingMailSender sender;

    public DevMailController(LoggingMailSender sender) {
        this.sender = sender;
    }

    @Anonymous
    @GetMapping("/latest")
    public ResponseEntity<EmailMessage> latest(@RequestParam String to) {
        return sender.last(to)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
