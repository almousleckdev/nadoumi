package com.nadoumi.payment.web.response;

import com.nadoumi.payment.domain.ApplicationPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** The staff view of a payment for the verification queue. No media id or storage URL is ever included. */
public record StaffPaymentResponse(
        Long id,
        Long applicationId,
        String applicationRef,
        String applicantName,
        String kind,
        BigDecimal amount,
        String currency,
        String reference,
        String status,
        String studentTxnNo,
        boolean hasProof,
        String createdAt,
        String submittedAt,
        String expiresAt,
        String verifiedAt,
        String rejectionReason) {

    public static StaffPaymentResponse of(ApplicationPayment p) {
        return new StaffPaymentResponse(
                p.getId(), p.getApplicationId(), p.getApplicationReference(), p.getApplicantName(), p.getKind(),
                p.getAmount(), p.getCurrency(), p.getReference(), p.getStatus().name(), p.getStudentTxnNo(),
                p.getProofMediaId() != null, utc(p.getCreateTime()), utc(p.getSubmittedAt()),
                utc(p.getExpiresAt()), utc(p.getVerifiedAt()), p.getRejectionReason());
    }

    private static String utc(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC).toString();
    }
}
