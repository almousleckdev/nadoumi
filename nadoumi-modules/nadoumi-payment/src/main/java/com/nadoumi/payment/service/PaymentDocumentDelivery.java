package com.nadoumi.payment.service;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.applicant.service.ApplicantDossierService;
import com.nadoumi.application.service.ApplicationOpportunityService;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.domain.enums.DocumentKind;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import com.nadoumi.payment.mapper.PaymentDocumentMapper.Recipient;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Emails the generated documents. Every send is preceded by a <em>claim</em> (a unique row per document and
 * recipient), so two workers, or a retry after a crash, can never send the same document to the same person
 * twice. A send that fails releases its claim and is retried on the next run.
 *
 * <p>The receipt goes to the student only. The submitted application form goes to the administrators only,
 * resolved from the configured role keys at send time; staff receive nothing.</p>
 */
@Service
public class PaymentDocumentDelivery {

    private static final Logger log = LoggerFactory.getLogger(PaymentDocumentDelivery.class);
    private static final Duration STALE_CLAIM = Duration.ofMinutes(10);
    private static final int BATCH = 20;
    private static final String SYSTEM = "system";

    private final PaymentDocumentMapper documents;
    private final PaymentMapper payments;
    private final PaymentDocumentService documentService;
    private final PaymentDocumentMailer mailer;
    private final ApplicantDossierService dossiers;
    private final ApplicationOpportunityService opportunities;
    private final PaymentSettings settings;
    private final MailSender mail;
    private final Clock clock;

    public PaymentDocumentDelivery(PaymentDocumentMapper documents, PaymentMapper payments,
            PaymentDocumentService documentService, PaymentDocumentMailer mailer, ApplicantDossierService dossiers,
            ApplicationOpportunityService opportunities, PaymentSettings settings, MailSender mail, Clock clock) {
        this.documents = documents;
        this.payments = payments;
        this.documentService = documentService;
        this.mailer = mailer;
        this.dossiers = dossiers;
        this.opportunities = opportunities;
        this.settings = settings;
        this.mail = mail;
        this.clock = clock;
    }

    /** Sends each unsent receipt to the student who paid. Returns how many documents were completed. */
    public int deliverReceipts() {
        int done = 0;
        for (PaymentDocument receipt : documents.findUndelivered(DocumentKind.RECEIPT,
                PaymentDocumentService.FIRST_VERSION, BATCH)) {
            try {
                done += deliverReceipt(receipt) ? 1 : 0;
            }
            catch (RuntimeException e) {
                log.warn("receipt delivery failed for document {}; will retry", receipt.getId(), e);
            }
        }
        return done;
    }

    /** Sends each unsent submitted application form to the administrators. */
    public int deliverSubmittedForms() {
        int done = 0;
        for (PaymentDocument form : documents.findUndelivered(DocumentKind.APPLICATION_FORM,
                PaymentDocumentService.SUBMITTED_VERSION, BATCH)) {
            try {
                done += deliverForm(form) ? 1 : 0;
            }
            catch (RuntimeException e) {
                log.warn("application form delivery failed for document {}; will retry", form.getId(), e);
            }
        }
        return done;
    }

    private boolean deliverReceipt(PaymentDocument receipt) {
        ApplicationPayment payment = payments.findById(receipt.getPaymentId());
        ApplicantDossier dossier = dossiers.read(payment.getApplicantId());
        Recipient student = receiptRecipient(payment, dossier);
        if (student == null) {
            log.warn("no verified email to send the receipt of payment {} to", payment.getId());
            return false;
        }
        byte[] pdf = documentService.read(receipt, systemContext(payment));
        send(receipt, student.userId(), mailer.receipt(student.email(), dossier.profile().givenName(), payment, receipt, pdf));
        documents.markDelivered(receipt.getId(), now());
        return true;
    }

    private boolean deliverForm(PaymentDocument form) {
        List<Recipient> admins = documents.findActiveByRoleKeys(settings.notifyRoleKeys());
        if (admins.isEmpty()) {
            log.warn("no administrator with an email to send application form {} to", form.getId());
            return false;
        }
        ApplicationPayment payment = payments.findById(form.getPaymentId());
        ApplicantDossier dossier = dossiers.read(payment.getApplicantId());
        var opportunity = opportunities.describe(payment.getApplicationId());
        byte[] pdf = documentService.read(form, systemContext(payment));
        boolean allSent = true;
        for (Recipient admin : admins) {
            try {
                send(form, admin.userId(), mailer.submittedForm(admin.email(), payment, form, dossier, opportunity, pdf));
            }
            catch (RuntimeException e) {
                allSent = false;
                log.warn("could not email application form {} to user {}", form.getId(), admin.userId(), e);
            }
        }
        if (allSent) {
            documents.markDelivered(form.getId(), now());
        }
        return allSent;
    }

    /** Claim, send, record. A duplicate claim means this recipient was already handled. */
    private void send(PaymentDocument document, long userId, EmailMessage message) {
        LocalDateTime now = now();
        documents.deleteStaleClaim(document.getId(), userId, now.minus(STALE_CLAIM));
        try {
            documents.claimDelivery(document.getId(), userId, now);
        }
        catch (DuplicateKeyException e) {
            return;
        }
        try {
            mail.send(message);
        }
        catch (RuntimeException e) {
            documents.releaseClaim(document.getId(), userId);
            throw e;
        }
        documents.markSent(document.getId(), userId, now());
    }

    /** The applicant's own verified email, else the account that paid. */
    private Recipient receiptRecipient(ApplicationPayment payment, ApplicantDossier dossier) {
        var profile = dossier.profile();
        if (profile.emailVerified() && profile.email() != null && !profile.email().isBlank()) {
            return new Recipient(payerId(payment), profile.email(), profile.givenName());
        }
        Recipient payer = documents.findRecipient(payerId(payment));
        return payer == null || payer.email() == null || payer.email().isBlank() ? null : payer;
    }

    private static long payerId(ApplicationPayment payment) {
        return Long.parseLong(payment.getCreateBy());
    }

    private static MediaAccessLogContext systemContext(ApplicationPayment payment) {
        return new MediaAccessLogContext(payment.getVerifiedBy() == null ? 0L : payment.getVerifiedBy(), null,
                payment.getApplicationId(), null, SYSTEM, SYSTEM);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }
}
