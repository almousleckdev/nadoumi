package com.nadoumi.payment.web.response;

import java.math.BigDecimal;

/**
 * What the student sees of an application's payment. {@code state} is one of NOT_REQUIRED, NOT_STARTED,
 * PENDING, PROOF_SUBMITTED, CONFIRMED, REJECTED, EXPIRED, CANCELLED. {@code secondsLeft} is computed by the
 * server (so a wrong device clock cannot extend the window) and is zero unless the state is PENDING.
 */
public record StudentPaymentResponse(
        String state,
        boolean required,
        boolean settled,
        boolean canStart,
        BigDecimal amount,
        String currency,
        String payee,
        String reference,
        String expiresAt,
        long secondsLeft,
        String rejectionReason) {
}
