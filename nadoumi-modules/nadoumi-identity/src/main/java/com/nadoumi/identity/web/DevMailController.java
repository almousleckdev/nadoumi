package com.nadoumi.identity.web;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.LoggingMailSender;
import com.ruoyi.common.annotation.Anonymous;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Non-production helper: returns the last email captured for a recipient. Mounted
 * only when {@code nadoumi.mail.transport=log} (dev / CI / tests), where
 * {@link LoggingMailSender} is the active {@code MailSender}. Used by the Playwright
 * {@code readOtp} helper.
 */
@RestController
@RequestMapping("/api/dev/mail")
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "log", matchIfMissing = true)
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
