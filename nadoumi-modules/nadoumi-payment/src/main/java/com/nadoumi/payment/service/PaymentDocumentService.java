package com.nadoumi.payment.service;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.applicant.service.ApplicantDossierService;
import com.nadoumi.application.service.ApplicationOpportunityService;
import com.nadoumi.application.service.ApplicationOpportunityService.Description;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.domain.enums.DocumentKind;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import com.nadoumi.payment.pdf.ApplicationFormModel;
import com.nadoumi.payment.pdf.ApplicationFormPdf;
import com.nadoumi.payment.pdf.ReceiptModel;
import com.nadoumi.payment.pdf.ReceiptPdf;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders and stores the payment receipt and the application form of a confirmed payment. The PDFs are media
 * assets (SENSITIVE, streamed by the backend); a row here records number, version, hash and who was sent what.
 *
 * <p>Version 1 of the form is made at confirmation so the student can download it; version 2 is made when the
 * student submits and is the copy the administrators receive.</p>
 */
@Service
public class PaymentDocumentService {

    public static final int FIRST_VERSION = 1;
    public static final int SUBMITTED_VERSION = 2;

    private static final Logger log = LoggerFactory.getLogger(PaymentDocumentService.class);
    private static final String PDF = "application/pdf";
    // the payee label may carry a Chinese name in brackets; the bundled font is Latin, so the receipt prints the Latin part
    private static final Pattern CJK_BRACKETS = Pattern.compile("\\s*[(（][^)）]*\\p{IsHan}[^)）]*[)）]");

    private final PaymentDocumentMapper documents;
    private final ReceiptPdf receiptPdf;
    private final ApplicationFormPdf formPdf;
    private final ApplicationOpportunityService opportunities;
    private final ApplicantDossierService dossiers;
    private final MediaGateway media;
    private final PaymentSettings settings;
    private final Clock clock;

    public PaymentDocumentService(PaymentDocumentMapper documents, ReceiptPdf receiptPdf, ApplicationFormPdf formPdf,
            ApplicationOpportunityService opportunities, ApplicantDossierService dossiers, MediaGateway media,
            PaymentSettings settings, Clock clock) {
        this.documents = documents;
        this.receiptPdf = receiptPdf;
        this.formPdf = formPdf;
        this.opportunities = opportunities;
        this.dossiers = dossiers;
        this.media = media;
        this.settings = settings;
        this.clock = clock;
    }

    /** The receipt and version 1 of the application form for a confirmed payment. Safe to call again. */
    @Transactional(rollbackFor = Exception.class)
    public void generateForConfirmed(ApplicationPayment payment) {
        Description opportunity = opportunities.describe(payment.getApplicationId());
        ApplicantDossier dossier = dossiers.read(payment.getApplicantId());
        Instant now = clock.instant();

        store(payment, DocumentKind.RECEIPT, FIRST_VERSION, PaymentDocumentNumbers.receipt(payment),
                receiptPdf.render(receiptModel(payment, opportunity, dossier, now)));
        store(payment, DocumentKind.APPLICATION_FORM, FIRST_VERSION, PaymentDocumentNumbers.applicationForm(payment),
                formPdf.render(formModel(payment, opportunity, dossier, now)));
    }

    /** The application form as it stands now, stored as the version the administrators receive. */
    @Transactional(rollbackFor = Exception.class)
    public void generateSubmittedForm(ApplicationPayment payment) {
        Description opportunity = opportunities.describe(payment.getApplicationId());
        ApplicantDossier dossier = dossiers.read(payment.getApplicantId());
        store(payment, DocumentKind.APPLICATION_FORM, SUBMITTED_VERSION, PaymentDocumentNumbers.applicationForm(payment),
                formPdf.render(formModel(payment, opportunity, dossier, clock.instant())));
    }

    /** The latest receipt and form of an application, newest version of each kind. */
    @Transactional(readOnly = true)
    public List<PaymentDocument> listFor(long applicationId) {
        return documents.findLatestByApplication(applicationId);
    }

    /** Opens the latest document of {@code kind} for an application, access-logged. */
    @Transactional(readOnly = true)
    public OpenedDocument open(long applicationId, DocumentKind kind, MediaAccessLogContext context) {
        PaymentDocument document = listFor(applicationId).stream()
                .filter(d -> d.getKind() == kind)
                .findFirst()
                .orElseThrow(() -> new NadNotFoundException("document not found"));
        if (document.getMediaId() == null) {
            throw new NadNotFoundException("document not found");
        }
        return new OpenedDocument(document, media.openProxyStream(document.getMediaId(), context));
    }

    /** How long a student must wait before the receipt can be emailed to them again. */
    public static final java.time.Duration RESEND_COOLDOWN = java.time.Duration.ofMinutes(5);

    /**
     * Has the receipt emailed again: forgets its deliveries so the documents job sends it on its next pass. Allowed
     * once the cooldown since the last send has passed, so it cannot be used to flood an inbox.
     */
    @Transactional(rollbackFor = Exception.class)
    public void resendReceipt(long applicationId) {
        PaymentDocument receipt = listFor(applicationId).stream()
                .filter(d -> d.getKind() == DocumentKind.RECEIPT)
                .findFirst()
                .orElseThrow(() -> new NadNotFoundException("document not found"));
        LocalDateTime last = documents.lastSentAt(receipt.getId());
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        if (last == null) {
            return; // never sent yet: the job is about to send it
        }
        if (last.plus(RESEND_COOLDOWN).isAfter(now)) {
            throw new com.nadoumi.payment.exception.PaymentThrottledException(
                    "the receipt was sent a moment ago; try again in a few minutes");
        }
        documents.clearDeliveries(receipt.getId());
        documents.markUndelivered(receipt.getId());
    }

    /** The bytes of a stored document, for attaching to an email. */
    public byte[] read(PaymentDocument document, MediaAccessLogContext context) {
        try (var stream = media.openProxyStream(document.getMediaId(), context).body()) {
            return stream.readAllBytes();
        }
        catch (IOException e) {
            throw new UncheckedIOException("could not read document " + document.getId(), e);
        }
    }

    public record OpenedDocument(PaymentDocument document, ProxyStream stream) {
    }

    private void store(ApplicationPayment payment, DocumentKind kind, int version, String number, byte[] pdf) {
        long uploader = payment.getVerifiedBy() == null ? 0L : payment.getVerifiedBy();
        long mediaId = media.upload(new ByteArrayInputStream(pdf), filename(kind, number), PDF, pdf.length,
                MediaCategory.PAYMENT_DOCUMENT, null, new MediaOwnerRef(MediaOwnerKind.PAYMENT, payment.getId()),
                uploader).mediaId();

        PaymentDocument row = new PaymentDocument();
        row.setPaymentId(payment.getId());
        row.setApplicationId(payment.getApplicationId());
        row.setKind(kind);
        row.setVersion(version);
        row.setDocNumber(number);
        row.setMediaId(mediaId);
        row.setSha256(sha256(pdf));
        row.setByteSize(pdf.length);
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        row.setGeneratedAt(now);
        if (kind == DocumentKind.APPLICATION_FORM && version == FIRST_VERSION) {
            row.setDeliveredAt(now); // the student's own copy: nothing is emailed
        }
        row.setCreateBy("system");
        row.setCreateTime(now);
        try {
            documents.insert(row);
        }
        catch (DuplicateKeyException e) {
            // another worker rendered this version first: keep theirs, drop our upload
            media.softDelete(mediaId, uploader);
            log.debug("payment {} {} v{} already rendered", payment.getId(), kind, version);
        }
    }

    private ReceiptModel receiptModel(ApplicationPayment payment, Description opportunity, ApplicantDossier dossier,
            Instant now) {
        var profile = dossier.profile();
        return new ReceiptModel(PaymentDocumentNumbers.receipt(payment), now, studentName(payment, dossier),
                profile.publicId(), profile.email(), applicationRef(payment), opportunity.title(),
                opportunity.universityName(), opportunity.intake(), payment.getReference(),
                payment.getStudentTxnNo(), payment.getVerifiedAt().toInstant(ZoneOffset.UTC), payment.getAmount(),
                payment.getCurrency(), CJK_BRACKETS.matcher(settings.payee()).replaceAll(""));
    }

    private ApplicationFormModel formModel(ApplicationPayment payment, Description opportunity,
            ApplicantDossier dossier, Instant now) {
        return new ApplicationFormModel(PaymentDocumentNumbers.applicationForm(payment), now, applicationRef(payment),
                opportunity, payment.getReference(), dossier);
    }

    private static String studentName(ApplicationPayment payment, ApplicantDossier dossier) {
        String given = dossier.profile().givenName();
        String family = dossier.profile().familyName();
        String full = ((given == null ? "" : given) + " " + (family == null ? "" : family)).strip();
        return full.isEmpty() ? payment.getApplicantName() : full;
    }

    static String applicationRef(ApplicationPayment payment) {
        return "APP-" + payment.getApplicationId();
    }

    private static String filename(DocumentKind kind, String number) {
        return (kind == DocumentKind.RECEIPT ? "NADOUMI-RECEIPT-" : "NADOUMI-APPLICATION-") + number + ".pdf";
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
