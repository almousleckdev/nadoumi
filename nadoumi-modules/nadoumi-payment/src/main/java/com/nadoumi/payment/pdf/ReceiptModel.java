package com.nadoumi.payment.pdf;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Everything a receipt prints. It deliberately has no field for a university beyond {@code universityName}, which
 * its builder fills only when the application description allows it (see ApplicationOpportunityService).
 */
public record ReceiptModel(
        String receiptNo,
        Instant issuedAt,
        String studentName,
        String studentPublicId,
        String studentEmail,
        String applicationRef,
        String opportunityTitle,
        String universityName,
        String intake,
        String paymentReference,
        String transactionNo,
        Instant paidAt,
        BigDecimal amount,
        String currency,
        String payee,
        /** What was paid for: {@link #APPLICATION_FEE_NAME} or {@link #SERVICE_FEE_NAME}. */
        String serviceName,
        /** The address the printed QR code opens, or null for none. */
        String verifyUrl) {

    public static final String APPLICATION_FEE_NAME = "Nadoumi application fee";
    public static final String SERVICE_FEE_NAME = "Nadoumi service fee";
}
