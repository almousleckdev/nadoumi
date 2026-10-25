package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.finance.service.RevenueService;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.exception.PaymentConflictException;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentVerificationServiceTest {

    private static final long PAYMENT_ID = 100L;
    private static final long STAFF_ID = 3L;
    private static final long STUDENT_USER_ID = 5L;
    private static final Instant NOW_INSTANT = Instant.parse("2026-10-22T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);

    private final PaymentMapper mapper = mock(PaymentMapper.class);
    private final RevenueService revenue = mock(RevenueService.class);
    private final PaymentEvents events = mock(PaymentEvents.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PaymentVerificationService service = new PaymentVerificationService(
            mapper, mock(MediaGateway.class), revenue, events, caller, Clock.fixed(NOW_INSTANT, ZoneOffset.UTC),
            mock(com.nadoumi.payment.mapper.PaymentDocumentMapper.class));

    @BeforeEach
    void setUp() {
        when(caller.requireUserId()).thenReturn(STAFF_ID);
    }

    private ApplicationPayment payment(PaymentStatus status) {
        ApplicationPayment p = new ApplicationPayment();
        p.setId(PAYMENT_ID);
        p.setApplicationId(41L);
        p.setApplicantId(8L);
        p.setKind("APPLICATION_FEE");
        p.setAmount(new BigDecimal("300.00"));
        p.setCurrency("CNY");
        p.setReference("NAD-7K3Q9FXT");
        p.setStatus(status);
        p.setVersion(4);
        p.setCreateBy(String.valueOf(STUDENT_USER_ID));
        when(mapper.findById(PAYMENT_ID)).thenReturn(p);
        return p;
    }

    @Test
    void shouldConfirm_bookTheFeeAsRevenue_andTellTheStudent() {
        ApplicationPayment p = payment(PaymentStatus.PROOF_SUBMITTED);
        when(mapper.markConfirmed(PAYMENT_ID, 4, STAFF_ID, NOW)).thenReturn(1);

        service.confirm(PAYMENT_ID);

        verify(mapper).insertEvent(PAYMENT_ID, PaymentStatus.PROOF_SUBMITTED, PaymentStatus.CONFIRMED, "STAFF",
                STAFF_ID, null, NOW);
        verify(revenue).recordApplicationFee(new BigDecimal("300.00"), "CNY", NOW.toLocalDate(),
                "NAD-7K3Q9FXT", 41L, STAFF_ID);
        verify(events).confirmed(p);
    }

    @Test
    void shouldHaveNoSideEffects_whenAnotherStaffMemberDecidedFirst() {
        payment(PaymentStatus.PROOF_SUBMITTED);
        when(mapper.markConfirmed(PAYMENT_ID, 4, STAFF_ID, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.confirm(PAYMENT_ID)).isInstanceOf(PaymentConflictException.class);

        verify(revenue, never()).recordApplicationFee(any(), any(), any(), any(), anyLong(), anyLong());
        verify(events, never()).confirmed(any());
    }

    @Test
    void shouldOnlyDecideAPaymentWhoseProofWasSubmitted() {
        payment(PaymentStatus.PENDING);

        assertThatThrownBy(() -> service.confirm(PAYMENT_ID)).isInstanceOf(PaymentConflictException.class);
        assertThatThrownBy(() -> service.reject(PAYMENT_ID, "Amount does not match"))
                .isInstanceOf(PaymentConflictException.class);
        verify(mapper, never()).markConfirmed(anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    void shouldNotLetAStaffMemberVerifyAPaymentOpenedByTheirOwnAccount() {
        payment(PaymentStatus.PROOF_SUBMITTED).setCreateBy(String.valueOf(STAFF_ID));

        assertThatThrownBy(() -> service.confirm(PAYMENT_ID)).isInstanceOf(NadForbiddenException.class);
        verify(mapper, never()).markConfirmed(anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    void shouldReject_withTheReasonShownToTheStudent() {
        ApplicationPayment p = payment(PaymentStatus.PROOF_SUBMITTED);
        when(mapper.markRejected(PAYMENT_ID, 4, STAFF_ID, "Amount does not match", NOW)).thenReturn(1);

        service.reject(PAYMENT_ID, "  Amount does not match ");

        verify(events).rejected(p, "Amount does not match");
        verify(revenue, never()).recordApplicationFee(any(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void shouldRequireAReasonToReject() {
        payment(PaymentStatus.PROOF_SUBMITTED);

        assertThatThrownBy(() -> service.reject(PAYMENT_ID, " ")).isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> service.reject(PAYMENT_ID, "x".repeat(301))).isInstanceOf(NadBadRequestException.class);
        assertThat(true).isTrue();
    }
}
