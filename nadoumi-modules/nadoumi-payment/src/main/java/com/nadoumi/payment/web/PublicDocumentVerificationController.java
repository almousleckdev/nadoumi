package com.nadoumi.payment.web;

import com.nadoumi.payment.service.DocumentVerification;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public check of a printed document. It answers valid or not, plus the kind, the number and the day it was issued;
 * never a name, an amount or an application. Rate-limited per IP so the code cannot be guessed by brute force.
 */
@RestController
@RequestMapping("/api/public/documents")
public class PublicDocumentVerificationController {

    /** The answer. {@code valid} is false, and the rest null, for any pair that does not match. */
    public record Verification(boolean valid, String kind, String number, String issuedOn) {
    }

    private static final Verification INVALID = new Verification(false, null, null, null);

    private final DocumentVerification verification;

    public PublicDocumentVerificationController(DocumentVerification verification) {
        this.verification = verification;
    }

    @Anonymous
    @GetMapping("/verify")
    @RateLimiter(count = 20, time = 60, limitType = LimitType.IP)
    public Verification verify(@RequestParam String number, @RequestParam String code) {
        return verification.verify(number, code)
                .map(d -> new Verification(true, d.getKind().name(), d.getDocNumber(),
                        d.getGeneratedAt().toLocalDate().toString()))
                .orElse(INVALID);
    }
}
