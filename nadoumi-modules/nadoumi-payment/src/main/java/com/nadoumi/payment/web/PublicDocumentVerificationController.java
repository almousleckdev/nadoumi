package com.nadoumi.payment.web;

import com.nadoumi.payment.service.DocumentVerification;
import com.nadoumi.payment.service.VerifiedDocumentDetails;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public check of a printed document. It answers valid or not, plus the kind, the number, when it was issued and, for a
 * valid pair, the student's name, the programme and the fee line (see {@link VerifiedDocumentDetails}). The unguessable
 * code is the only key, and the endpoint is rate-limited per IP so it cannot be brute forced.
 */
@RestController
@RequestMapping("/api/public/documents")
public class PublicDocumentVerificationController {

    /** The answer. {@code valid} is false, and the rest null, for any pair that does not match. */
    public record Verification(boolean valid, String kind, String number, String issuedOn, VerifiedDocumentDetails.Details details) {
    }

    private static final Verification INVALID = new Verification(false, null, null, null, null);

    private final DocumentVerification verification;
    private final VerifiedDocumentDetails details;

    public PublicDocumentVerificationController(DocumentVerification verification, VerifiedDocumentDetails details) {
        this.verification = verification;
        this.details = details;
    }

    @Anonymous
    @GetMapping("/verify")
    @RateLimiter(count = 20, time = 60, limitType = LimitType.IP)
    public Verification verify(@RequestParam String number, @RequestParam String code) {
        return verification.verify(number, code)
                .map(d -> new Verification(true, d.getKind().name(), d.getDocNumber(),
                        d.getGeneratedAt().toLocalDate().toString(), details.of(d).orElse(null)))
                .orElse(INVALID);
    }
}
