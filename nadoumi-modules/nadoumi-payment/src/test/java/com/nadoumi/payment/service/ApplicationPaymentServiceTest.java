package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.service.ApplicationReadService;
import com.nadoumi.application.service.ApplicationReadService.ApplicationFacts;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.exception.PaymentConflictException;
import com.nadoumi.payment.mapper.PaymentMapper;
import com.nadoumi.payment.web.response.StudentPaymentResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

class ApplicationPaymentServiceTest {

    private static final long APPLICATION_ID = 41L;
    private static final long APPLICANT_ID = 8L;
    private static final long USER_ID = 5L;
    private static final String KIND = "APPLICATION_FEE";
    private static final Instant NOW_INSTANT = Instant.parse("2026-10-22T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);

    private final PaymentMapper mapper = mock(PaymentMapper.class);
    private final PaymentSettings settings = mock(PaymentSettings.class);
    private final ApplicationReadService applications = mock(ApplicationReadService.class);
    private final ApplicantNames names = mock(ApplicantNames.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final PaymentEvents events = mock(PaymentEvents.class);
    private final PaymentReferences references = mock(PaymentReferences.class);
    private final PaymentQrSource qr = mock(PaymentQrSource.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final Clock clock = Clock.fixed(NOW_INSTANT, ZoneOffset.UTC);

    private final ApplicationPaymentService service = new ApplicationPaymentService(
            mapper, settings, applications, names, media, events, references, qr, caller, clock);

    @BeforeEach
    void setUp() {
        when(settings.applicationFee()).thenReturn(new BigDecimal("300.00"));
        when(settings.payee()).thenReturn("Nadoumi");
        when(applications.find(APPLICATION_ID)).thenReturn(Optional.of(
                new ApplicationFacts(APPLICATION_ID, "NAD20261023001", APPLICANT_ID, "DRAFT", true)));
        when(caller.requireUserId()).thenReturn(USER_ID);
        when(names.of(APPLICANT_ID)).thenReturn("Ada Lovelace");
        when(references.next()).thenReturn("NAD-7K3Q9FXT");
        // like the database: assign the generated key
        when(mapper.insert(any())).thenAnswer(call -> {
            call.<ApplicationPayment>getArgument(0).setId(100L);
            return 1;
        });
    }

    private ApplicationPayment payment(PaymentStatus status, LocalDateTime expiresAt) {
        ApplicationPayment p = new ApplicationPayment();
        p.setId(100L);
        p.setApplicationId(APPLICATION_ID);
        p.setApplicantId(APPLICANT_ID);
        p.setKind(KIND);
        p.setAmount(new BigDecimal("300.00"));
        p.setCurrency("CNY");
        p.setReference("NAD-7K3Q9FXT");
        p.setStatus(status);
        p.setExpiresAt(expiresAt);
        p.setVersion(2);
        return p;
    }

    private MockMultipartFile screenshot() {
        return new MockMultipartFile("file", "proof.png", "image/png", new byte[] {1, 2, 3});
    }

    // ---- the service fee ----

    @Test
    void shouldOnlyOpenAServiceFeeSession_afterTheAdmissionIsConfirmed() {
        when(applications.serviceFee(APPLICATION_ID)).thenReturn(new BigDecimal("1500.00"));
        when(applications.find(APPLICATION_ID)).thenReturn(Optional.of(
                new ApplicationFacts(APPLICATION_ID, "NAD20261023001", APPLICANT_ID, "OFFER_ACCEPTED", false)));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(APPLICATION_ID, "SERVICE_FEE"))
                .isInstanceOf(com.nadoumi.payment.exception.PaymentConflictException.class);
        verify(mapper, never()).insert(any());

        when(applications.find(APPLICATION_ID)).thenReturn(Optional.of(
                new ApplicationFacts(APPLICATION_ID, "NAD20261023001", APPLICANT_ID, "ADMISSION_CONFIRMED", false)));
        service.start(APPLICATION_ID, "SERVICE_FEE");

        ArgumentCaptor<ApplicationPayment> saved = ArgumentCaptor.forClass(ApplicationPayment.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getKind()).isEqualTo("SERVICE_FEE");
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldNotAskForAServiceFee_whenTheOpportunityHasNone() {
        when(applications.serviceFee(APPLICATION_ID)).thenReturn(BigDecimal.ZERO);

        StudentPaymentResponse r = service.start(APPLICATION_ID, "SERVICE_FEE");

        assertThat(r.state()).isEqualTo("NOT_REQUIRED");
        verify(mapper, never()).insert(any());
    }

    // ---- starting a session ----

    @Test
    void shouldNotRequireAPayment_whenNoApplicationFeeIsConfigured() {
        when(settings.applicationFee()).thenReturn(BigDecimal.ZERO);

        StudentPaymentResponse r = service.start(APPLICATION_ID);

        assertThat(r.state()).isEqualTo("NOT_REQUIRED");
        assertThat(r.required()).isFalse();
        assertThat(r.settled()).isTrue();
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldOpenAFifteenMinuteSession_withTheFeeSnapshotAndAReference() {
        service.start(APPLICATION_ID);

        ArgumentCaptor<ApplicationPayment> saved = ArgumentCaptor.forClass(ApplicationPayment.class);
        verify(mapper).insert(saved.capture());
        ApplicationPayment p = saved.getValue();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.getAmount()).isEqualByComparingTo("300.00");
        assertThat(p.getCurrency()).isEqualTo("CNY");
        assertThat(p.getReference()).isEqualTo("NAD-7K3Q9FXT");
        assertThat(p.getApplicantName()).isEqualTo("Ada Lovelace");
        assertThat(p.getExpiresAt()).isEqualTo(NOW.plusMinutes(15));
        assertThat(p.getCreateBy()).isEqualTo(String.valueOf(USER_ID));
    }

    @Test
    void shouldReturnTheOpenSession_insteadOfOpeningASecondOne() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(9)));
        when(mapper.findLatest(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(9)));

        StudentPaymentResponse r = service.start(APPLICATION_ID);

        assertThat(r.state()).isEqualTo("PENDING");
        assertThat(r.secondsLeft()).isEqualTo(9 * 60);
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldRefuseToOpenASession_whenTheApplicationIsSubmittedAndItsLastPaymentWasNotRejected() {
        when(applications.find(APPLICATION_ID)).thenReturn(Optional.of(
                new ApplicationFacts(APPLICATION_ID, "NAD20261023001", APPLICANT_ID, "IN_REVIEW", false)));
        when(mapper.findLatest(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.EXPIRED, NOW.minusDays(1)));

        assertThatThrownBy(() -> service.start(APPLICATION_ID)).isInstanceOf(PaymentConflictException.class);
    }

    @Test
    void shouldNotOpenAnotherSession_whenThePaymentIsAlreadyConfirmed() {
        when(mapper.countConfirmed(APPLICATION_ID, KIND)).thenReturn(1);
        when(mapper.findLatest(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.CONFIRMED, NOW.minusDays(1)));

        StudentPaymentResponse r = service.start(APPLICATION_ID);

        assertThat(r.state()).isEqualTo("CONFIRMED");
        assertThat(r.settled()).isTrue();
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldExpireAnOverdueSession_andOpenAFreshOne() {
        ApplicationPayment overdue = payment(PaymentStatus.PENDING, NOW.minusSeconds(1));
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(overdue);
        when(mapper.markExpired(100L, NOW)).thenReturn(1);

        service.start(APPLICATION_ID);

        verify(mapper).markExpired(100L, NOW);
        verify(mapper).insertEvent(100L, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "SYSTEM", null, null, NOW);
        verify(mapper).insert(any());
    }

    @Test
    void shouldKeepTheExistingSession_whenAConcurrentRequestOpenedItFirst() {
        org.mockito.Mockito.doThrow(new DuplicateKeyException("uk_payment_open")).when(mapper).insert(any());
        when(mapper.findOpen(APPLICATION_ID, KIND))
                .thenReturn(null)
                .thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(14)));
        when(mapper.findLatest(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(14)));

        StudentPaymentResponse r = service.start(APPLICATION_ID);

        assertThat(r.state()).isEqualTo("PENDING");
    }

    // ---- the QR ----

    @Test
    void shouldServeTheQr_onlyInsideAnOpenSession() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(5)));
        var image = new PaymentQrSource.QrImage(new byte[] {9}, "image/png");
        when(qr.load()).thenReturn(image);

        assertThat(service.qr(APPLICATION_ID)).isSameAs(image);
    }

    @Test
    void shouldRefuseTheQr_whenThereIsNoOpenSession() {
        assertThatThrownBy(() -> service.qr(APPLICATION_ID)).isInstanceOf(PaymentConflictException.class);
        verify(qr, never()).load();
    }

    @Test
    void shouldRefuseTheQr_afterTheWindowCloses() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.minusMinutes(1)));
        when(mapper.markExpired(100L, NOW)).thenReturn(1);

        assertThatThrownBy(() -> service.qr(APPLICATION_ID)).isInstanceOf(PaymentConflictException.class);
        verify(qr, never()).load();
    }

    // ---- reporting the payment ----

    @Test
    void shouldAcceptTheProof_insideTheWindow_andTellTheStaff() {
        ApplicationPayment open = payment(PaymentStatus.PENDING, NOW.plusMinutes(3));
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(open);
        when(media.upload(any(MediaGateway.UploadSource.class), anyString(), anyString(), anyLong(),
                eq(MediaCategory.PAYMENT_PROOF), any(), any(), eq(USER_ID))).thenReturn(new MediaUploadResult(77L, null));
        when(mapper.markProofSubmitted(100L, 2, "BANK1234567", 77L, NOW, String.valueOf(USER_ID))).thenReturn(1);
        when(mapper.findById(100L)).thenReturn(open);
        when(mapper.findLatest(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PROOF_SUBMITTED, NOW.plusMinutes(3)));

        StudentPaymentResponse r = service.submitProof(APPLICATION_ID, " bank1234567 ", screenshot());

        assertThat(r.state()).isEqualTo("PROOF_SUBMITTED");
        verify(mapper).insertEvent(100L, PaymentStatus.PENDING, PaymentStatus.PROOF_SUBMITTED, "STUDENT", USER_ID, null, NOW);
        verify(events).proofSubmitted(open, USER_ID);
    }

    @Test
    void shouldRefuseTheProof_whenTheWindowHasClosed_andNeverStoreTheScreenshot() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.minusSeconds(1)));
        when(mapper.markExpired(100L, NOW)).thenReturn(1);

        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "BANK1234567", screenshot()))
                .isInstanceOf(PaymentConflictException.class).hasMessageContaining("window");
        verify(media, never()).upload(any(MediaGateway.UploadSource.class), anyString(), anyString(), anyLong(),
                any(), any(), any(), anyLong());
    }

    @Test
    void shouldRefuseTheProof_whenTheWindowClosesWhileUploading_andDiscardTheScreenshot() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusSeconds(2)));
        when(media.upload(any(MediaGateway.UploadSource.class), anyString(), anyString(), anyLong(),
                any(), any(), any(), anyLong())).thenReturn(new MediaUploadResult(77L, null));
        when(mapper.markProofSubmitted(anyLong(), org.mockito.ArgumentMatchers.anyInt(), anyString(), anyLong(),
                any(), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "BANK1234567", screenshot()))
                .isInstanceOf(PaymentConflictException.class);
        verify(media).softDelete(77L, USER_ID);
    }

    @Test
    void shouldRefuseTheProof_whenTheTransactionNumberWasAlreadyReported_andDiscardTheScreenshot() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(3)));
        when(media.upload(any(MediaGateway.UploadSource.class), anyString(), anyString(), anyLong(),
                any(), any(), any(), anyLong())).thenReturn(new MediaUploadResult(77L, null));
        when(mapper.markProofSubmitted(anyLong(), org.mockito.ArgumentMatchers.anyInt(), anyString(), anyLong(),
                any(), anyString())).thenThrow(new DuplicateKeyException("uk_payment_txn"));

        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "BANK1234567", screenshot()))
                .isInstanceOf(PaymentConflictException.class).hasMessageContaining("already");
        verify(media).softDelete(77L, USER_ID);
    }

    @Test
    void shouldRejectAMalformedTransactionNumber() {
        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "12", screenshot()))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "ABC DEF 123; drop", screenshot()))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRequireAScreenshot() {
        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "BANK1234567", null))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> service.submitProof(APPLICATION_ID, "BANK1234567",
                new MockMultipartFile("file", "x.png", "image/png", new byte[0])))
                .isInstanceOf(NadBadRequestException.class);
    }

    // ---- cancelling ----

    @Test
    void shouldCancelAPendingSession() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PENDING, NOW.plusMinutes(4)));
        when(mapper.markCancelled(100L, 2, NOW, String.valueOf(USER_ID))).thenReturn(1);

        service.cancel(APPLICATION_ID);

        verify(mapper).insertEvent(100L, PaymentStatus.PENDING, PaymentStatus.CANCELLED, "STUDENT", USER_ID, null, NOW);
    }

    @Test
    void shouldNotCancel_oncetheProofIsReported() {
        when(mapper.findOpen(APPLICATION_ID, KIND)).thenReturn(payment(PaymentStatus.PROOF_SUBMITTED, NOW.plusMinutes(4)));

        assertThatThrownBy(() -> service.cancel(APPLICATION_ID)).isInstanceOf(PaymentConflictException.class);
        verify(mapper, never()).markCancelled(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(), anyString());
    }

    // ---- reading ----

    @Test
    void shouldShowTheLatestRejection_andAllowStartingAgain() {
        ApplicationPayment rejected = payment(PaymentStatus.REJECTED, NOW.minusHours(1));
        rejected.setRejectionReason("Amount does not match");
        when(mapper.findLatest(APPLICATION_ID, KIND)).thenReturn(rejected);

        StudentPaymentResponse r = service.overview(APPLICATION_ID);

        assertThat(r.state()).isEqualTo("REJECTED");
        assertThat(r.rejectionReason()).isEqualTo("Amount does not match");
        assertThat(r.canStart()).isTrue();
        assertThat(r.settled()).isFalse();
    }

    @Test
    void shouldReportNotStarted_whenTheStudentNeverOpenedTheQr() {
        StudentPaymentResponse r = service.overview(APPLICATION_ID);

        assertThat(r.state()).isEqualTo("NOT_STARTED");
        assertThat(r.canStart()).isTrue();
        assertThat(r.amount()).isEqualByComparingTo("300.00");
    }
}
