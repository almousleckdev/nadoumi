package com.nadoumi.payment.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.application.service.PaymentGuardProvider;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.finance.service.RevenueService;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.exception.PaymentConflictException;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import com.nadoumi.payment.mapper.PaymentMapper;
import com.nadoumi.payment.web.response.StaffPaymentResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The staff side: list payments waiting for verification, view the proof, then confirm or reject.
 *
 * <p>Confirm and reject are one conditional update each ({@code where status = PROOF_SUBMITTED and version = ?}).
 * Two staff members pressing Confirm together cannot both succeed, so the ledger entry, the event and the
 * student notification happen exactly once. A staff member cannot verify a payment opened by their own account.</p>
 */
@Service
public class PaymentVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentVerificationService.class);
    private static final Set<String> KINDS = Set.of(PaymentGuardProvider.APPLICATION_FEE, PaymentGuardProvider.SERVICE_FEE);

    private final PaymentMapper mapper;
    private final MediaGateway media;
    private final RevenueService revenue;
    private final PaymentEvents events;
    private final CurrentCaller caller;
    private final Clock clock;
    private final PaymentDocumentMapper documents;

    public PaymentVerificationService(PaymentMapper mapper, MediaGateway media, RevenueService revenue,
            PaymentEvents events, CurrentCaller caller, Clock clock, PaymentDocumentMapper documents) {
        this.documents = documents;
        this.mapper = mapper;
        this.media = media;
        this.revenue = revenue;
        this.events = events;
        this.caller = caller;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffPaymentResponse> list(PaymentStatus status, String q, Long applicationId, String kind, int page,
            int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<ApplicationPayment> rows = mapper.search(status, Texts.blankToNull(q), applicationId, kind != null && KINDS.contains(kind) ? kind : null);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(StaffPaymentResponse::of).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public StaffPaymentResponse get(long id) {
        return StaffPaymentResponse.of(load(id));
    }

    /** Streams the screenshot through the backend; the storage URL is never handed out. */
    @Transactional(readOnly = true)
    public ProxyStream openProof(long id, MediaAccessLogContext context) {
        ApplicationPayment payment = load(id);
        if (payment.getProofMediaId() == null) {
            throw new NadNotFoundException("this payment has no proof");
        }
        return media.openProxyStream(payment.getProofMediaId(), context);
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffPaymentResponse confirm(long id) {
        ApplicationPayment payment = loadForDecision(id);
        long verifier = caller.requireUserId();
        LocalDateTime now = LocalDateTime.now(clock);
        if (mapper.markConfirmed(id, payment.getVersion(), verifier, now) == 0) {
            throw new PaymentConflictException("this payment was already decided");
        }
        mapper.insertEvent(id, PaymentStatus.PROOF_SUBMITTED, PaymentStatus.CONFIRMED, "STAFF", verifier, null, now);
        revenue.recordPayment(revenueSource(payment), payment.getAmount(), payment.getCurrency(), now.toLocalDate(),
                payment.getReference(), payment.getApplicationId(), verifier);
        ApplicationPayment confirmed = load(id);
        events.confirmed(confirmed);
        return StaffPaymentResponse.of(confirmed);
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffPaymentResponse reject(long id, String reason) {
        String cleaned = reason == null ? "" : reason.strip();
        if (cleaned.length() < 3 || cleaned.length() > 300) {
            throw new NadBadRequestException("give the student a reason of 3 to 300 characters");
        }
        ApplicationPayment payment = loadForDecision(id);
        long verifier = caller.requireUserId();
        LocalDateTime now = LocalDateTime.now(clock);
        if (mapper.markRejected(id, payment.getVersion(), verifier, cleaned, now) == 0) {
            throw new PaymentConflictException("this payment was already decided");
        }
        mapper.insertEvent(id, PaymentStatus.PROOF_SUBMITTED, PaymentStatus.REJECTED, "STAFF", verifier, cleaned, now);
        ApplicationPayment rejected = load(id);
        events.rejected(rejected, cleaned);
        return StaffPaymentResponse.of(rejected);
    }

    /**
     * Deletes a payment that was never confirmed (a wrong or fraudulent report, a test): its proof and receipts go with it.
     * A confirmed payment is booked revenue and is refused. If the application was submitted with this payment, the student
     * can then pay again.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id, String reason) {
        ApplicationPayment payment = load(id);
        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            throw new PaymentConflictException("a confirmed payment is booked revenue and cannot be deleted");
        }
        long actor = caller.requireUserId();
        documents.findMediaIdsByPayment(id).forEach(mediaId -> media.softDelete(mediaId, actor));
        documents.deleteByPaymentId(id);
        if (payment.getProofMediaId() != null) {
            media.softDelete(payment.getProofMediaId(), actor);
        }
        mapper.deleteById(id);
        log.warn("payment {} ({}, application {}) deleted by user {}; status {}; reason: {}", id, payment.getReference(),
                payment.getApplicationReference(), actor, payment.getStatus(), reason);
    }

    private static String revenueSource(ApplicationPayment payment) {
        return PaymentGuardProvider.SERVICE_FEE.equals(payment.getKind()) ? RevenueService.SOURCE_SERVICE_FEE
                : RevenueService.SOURCE_APPLICATION_FEE;
    }

    /** A payment that is waiting for a decision, and that the caller is allowed to decide. */
    private ApplicationPayment loadForDecision(long id) {
        ApplicationPayment payment = load(id);
        if (payment.getStatus() != PaymentStatus.PROOF_SUBMITTED) {
            throw new PaymentConflictException("only a payment with a submitted proof can be decided");
        }
        if (String.valueOf(caller.requireUserId()).equals(payment.getCreateBy())) {
            throw new NadForbiddenException("you cannot verify a payment opened by your own account");
        }
        return payment;
    }

    private ApplicationPayment load(long id) {
        ApplicationPayment payment = mapper.findById(id);
        if (payment == null || !KINDS.contains(payment.getKind())) {
            throw new NadNotFoundException("payment not found");
        }
        return payment;
    }
}
