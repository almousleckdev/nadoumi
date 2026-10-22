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
        String payee) {
}
