package com.nadoumi.payment.service;

import com.nadoumi.application.service.ApplicationOpportunityService;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the QR code of a printed document reveals once its number and code match: enough to check the paper against the
 * person holding it. Only the name, the programme and the fee line are shown; no contact, passport or account detail.
 */
@Component
public class VerifiedDocumentDetails {

    /** Times are UTC instants in ISO form; the page formats them for the reader. */
    public record Details(String applicantName, String applicationReference, String opportunity, String universityName,
            String intake, BigDecimal feeAmount, String currency, String feeStatus, String feePaidAt, String issuedAt) {
    }

    private final PaymentMapper payments;
    private final ApplicationOpportunityService opportunities;
    private final ApplicantNames names;

    public VerifiedDocumentDetails(PaymentMapper payments, ApplicationOpportunityService opportunities, ApplicantNames names) {
        this.payments = payments;
        this.opportunities = opportunities;
        this.names = names;
    }

    @Transactional(readOnly = true)
    public Optional<Details> of(PaymentDocument document) {
        ApplicationPayment payment = payments.findById(document.getPaymentId());
        if (payment == null) {
            return Optional.empty();
        }
        var opportunity = opportunities.describe(document.getApplicationId());
        LocalDateTime paidAt = payment.getVerifiedAt() != null ? payment.getVerifiedAt() : payment.getSubmittedAt();
        return Optional.of(new Details(names.of(payment.getApplicantId()), opportunity.reference(), opportunity.title(),
                opportunity.universityName(), opportunity.intake(), payment.getAmount(), payment.getCurrency(),
                payment.getStatus().name(), instant(paidAt), instant(document.getGeneratedAt())));
    }

    private static String instant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC).toString();
    }
}
