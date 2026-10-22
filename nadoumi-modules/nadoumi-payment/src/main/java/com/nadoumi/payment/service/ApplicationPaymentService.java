package com.nadoumi.payment.service;

import com.nadoumi.application.service.ApplicationReadService;
import com.nadoumi.application.service.ApplicationReadService.ApplicationFacts;
import com.nadoumi.application.service.PaymentGuardProvider;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.exception.PaymentConflictException;
import com.nadoumi.payment.mapper.PaymentMapper;
import com.nadoumi.payment.service.PaymentQrSource.QrImage;
import com.nadoumi.payment.web.response.StudentPaymentResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The student side of the Nadoumi application fee: open a payment session (the QR and a 15-minute window),
 * report the payment with a transaction number and a screenshot, or close the session.
 *
 * <p>An application stays a saved draft whether or not it is paid; only {@code submit} is gated (by
 * {@code PaymentGuard}). Every state change is a single conditional update, so concurrent requests cannot
 * produce two open payments or a proof after the window closed.</p>
 */
@Service
public class ApplicationPaymentService {

    private static final String KIND = PaymentGuardProvider.APPLICATION_FEE;
    private static final String METHOD_QR = "QR";
    private static final Pattern TXN_NO = Pattern.compile("[A-Za-z0-9_-]{6,40}");
    private static final int REFERENCE_ATTEMPTS = 3;

    private final PaymentMapper mapper;
    private final PaymentSettings settings;
    private final ApplicationReadService applications;
    private final ApplicantNames applicantNames;
    private final MediaGateway media;
    private final PaymentEvents events;
    private final PaymentReferences references;
    private final PaymentQrSource qr;
    private final CurrentCaller caller;
    private final Clock clock;

    public ApplicationPaymentService(PaymentMapper mapper, PaymentSettings settings,
            ApplicationReadService applications, ApplicantNames applicantNames, MediaGateway media,
            PaymentEvents events, PaymentReferences references, PaymentQrSource qr, CurrentCaller caller,
            Clock clock) {
        this.mapper = mapper;
        this.settings = settings;
        this.applications = applications;
        this.applicantNames = applicantNames;
        this.media = media;
        this.events = events;
        this.references = references;
        this.qr = qr;
        this.caller = caller;
        this.clock = clock;
    }

    /** The current payment state of an application. Also closes a session whose window has run out. */
    @Transactional(rollbackFor = Exception.class)
    public StudentPaymentResponse overview(long applicationId) {
        ApplicationFacts app = requireApplication(applicationId);
        expireIfOverdue(mapper.findOpen(applicationId, KIND));
        return respond(app);
    }

    /** Opens a payment session, or returns the one that is already open or confirmed. */
    @Transactional(rollbackFor = Exception.class)
    public StudentPaymentResponse start(long applicationId) {
        ApplicationFacts app = requireApplication(applicationId);
        BigDecimal fee = settings.applicationFee();
        if (fee.signum() <= 0) {
            return respond(app);
        }
        if (!app.draft()) {
            throw new PaymentConflictException("a payment can only be started while the application is a draft");
        }
        ApplicationPayment open = expireIfOverdue(mapper.findOpen(applicationId, KIND));
        if (open != null || mapper.countConfirmed(applicationId, KIND) > 0) {
            return respond(app);
        }
        insertSession(app, fee);
        return respond(app);
    }

    /** The QR image, only while the student's session is open. */
    @Transactional(rollbackFor = Exception.class)
    public QrImage qr(long applicationId) {
        requireApplication(applicationId);
        ApplicationPayment open = expireIfOverdue(mapper.findOpen(applicationId, KIND));
        if (open == null || open.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentConflictException("there is no open payment session; start a payment first");
        }
        return qr.load();
    }

    /** Reports the payment: the bank transaction number and a screenshot, inside the 15-minute window. */
    @Transactional(rollbackFor = Exception.class)
    public StudentPaymentResponse submitProof(long applicationId, String transactionNo, MultipartFile proof) {
        ApplicationFacts app = requireApplication(applicationId);
        String txn = normalizeTxn(transactionNo);
        if (proof == null || proof.isEmpty()) {
            throw new NadBadRequestException("a screenshot of the payment is required");
        }
        ApplicationPayment payment = expireIfOverdue(mapper.findOpen(applicationId, KIND));
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentConflictException("the payment window has closed; start a new payment");
        }
        long userId = caller.requireUserId();
        long mediaId = media.upload(proof::getInputStream, proof.getOriginalFilename(), proof.getContentType(),
                proof.getSize(), MediaCategory.PAYMENT_PROOF, null,
                new MediaOwnerRef(MediaOwnerKind.PAYMENT, payment.getId()), userId).mediaId();

        LocalDateTime now = now();
        int changed;
        try {
            changed = mapper.markProofSubmitted(payment.getId(), payment.getVersion(), txn, mediaId, now,
                    String.valueOf(userId));
        }
        catch (DuplicateKeyException e) {
            media.softDelete(mediaId, userId);
            throw new PaymentConflictException("this transaction number was already reported");
        }
        if (changed == 0) {
            media.softDelete(mediaId, userId);
            throw new PaymentConflictException("the payment window has closed; start a new payment");
        }
        record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.PROOF_SUBMITTED, "STUDENT", userId, null);
        events.proofSubmitted(mapper.findById(payment.getId()), userId);
        return respond(app);
    }

    /** Closes the session. Once a proof is reported it can no longer be cancelled; staff decide. */
    @Transactional(rollbackFor = Exception.class)
    public StudentPaymentResponse cancel(long applicationId) {
        ApplicationFacts app = requireApplication(applicationId);
        ApplicationPayment payment = expireIfOverdue(mapper.findOpen(applicationId, KIND));
        if (payment == null) {
            return respond(app);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentConflictException("the payment was already reported and is being verified");
        }
        long userId = caller.requireUserId();
        if (mapper.markCancelled(payment.getId(), payment.getVersion(), now(), String.valueOf(userId)) == 1) {
            record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.CANCELLED, "STUDENT", userId, null);
        }
        return respond(app);
    }

    // ---- internals ----

    private void insertSession(ApplicationFacts app, BigDecimal fee) {
        ApplicationPayment payment = new ApplicationPayment();
        payment.setApplicationId(app.id());
        payment.setApplicantId(app.applicantId());
        payment.setApplicantName(applicantNames.of(app.applicantId()));
        payment.setKind(KIND);
        payment.setAmount(fee);
        payment.setCurrency(PaymentSettings.CURRENCY);
        payment.setMethod(METHOD_QR);
        payment.setStatus(PaymentStatus.PENDING);
        LocalDateTime now = now();
        payment.setExpiresAt(now.plus(PaymentSettings.PAYMENT_WINDOW));
        payment.setCreateBy(String.valueOf(caller.requireUserId()));
        payment.setCreateTime(now);
        for (int attempt = 1; attempt <= REFERENCE_ATTEMPTS; attempt++) {
            payment.setReference(references.next());
            try {
                mapper.insert(payment);
                record(payment.getId(), null, PaymentStatus.PENDING, "STUDENT", caller.requireUserId(), null);
                return;
            }
            catch (DuplicateKeyException e) {
                // a concurrent request opened the session first, or the reference collided
                if (mapper.findOpen(app.id(), KIND) != null) {
                    return;
                }
            }
        }
        throw new PaymentConflictException("could not open a payment session; please try again");
    }

    /** Moves an overdue {@code PENDING} payment to {@code EXPIRED}; returns the payment still open, or null. */
    private ApplicationPayment expireIfOverdue(ApplicationPayment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING
                || payment.getExpiresAt().isAfter(now())) {
            return payment;
        }
        if (mapper.markExpired(payment.getId(), now()) == 1) {
            record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.EXPIRED, "SYSTEM", null, null);
            return null;
        }
        return mapper.findOpen(payment.getApplicationId(), KIND);
    }

    private StudentPaymentResponse respond(ApplicationFacts app) {
        BigDecimal fee = settings.applicationFee();
        if (fee.signum() <= 0) {
            return new StudentPaymentResponse("NOT_REQUIRED", false, true, false, BigDecimal.ZERO,
                    PaymentSettings.CURRENCY, settings.payee(), null, null, 0, null);
        }
        ApplicationPayment latest = mapper.findLatest(app.id(), KIND);
        if (latest == null) {
            return new StudentPaymentResponse("NOT_STARTED", true, false, app.draft(), fee,
                    PaymentSettings.CURRENCY, settings.payee(), null, null, 0, null);
        }
        PaymentStatus status = latest.getStatus();
        boolean settled = status == PaymentStatus.CONFIRMED;
        boolean canStart = app.draft() && !status.isOpen() && !settled;
        long secondsLeft = status == PaymentStatus.PENDING
                ? Math.max(0, Duration.between(now(), latest.getExpiresAt()).getSeconds()) : 0;
        // while a session or confirmation exists the amount is the snapshot; otherwise the current fee for a new one
        BigDecimal amount = status.isOpen() || settled ? latest.getAmount() : fee;
        return new StudentPaymentResponse(status.name(), true, settled, canStart, amount, latest.getCurrency(),
                settings.payee(), latest.getReference(),
                latest.getExpiresAt().toInstant(ZoneOffset.UTC).toString(), secondsLeft,
                status == PaymentStatus.REJECTED ? latest.getRejectionReason() : null);
    }

    private ApplicationFacts requireApplication(long applicationId) {
        return applications.find(applicationId).orElseThrow(() -> new NadNotFoundException("application not found"));
    }

    private static String normalizeTxn(String raw) {
        String txn = raw == null ? "" : raw.strip();
        if (!TXN_NO.matcher(txn).matches()) {
            throw new NadBadRequestException("enter the transaction number from your payment (6 to 40 letters or digits)");
        }
        return txn.toUpperCase();
    }

    private void record(long paymentId, PaymentStatus from, PaymentStatus to, String actorKind, Long actorId,
            String note) {
        mapper.insertEvent(paymentId, from, to, actorKind, actorId, note, now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
