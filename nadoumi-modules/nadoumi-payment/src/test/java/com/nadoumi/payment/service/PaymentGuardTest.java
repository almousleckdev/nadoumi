package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentGuardTest {

    private final PaymentMapper mapper = mock(PaymentMapper.class);
    private final PaymentSettings settings = mock(PaymentSettings.class);
    private final PaymentGuard guard = new PaymentGuard(mapper, settings);

    @Test
    void shouldBeSettled_whenNoFeeIsConfigured() {
        when(settings.applicationFee()).thenReturn(BigDecimal.ZERO);

        assertThat(guard.isSettled(41L, "APPLICATION_FEE")).isTrue();
    }

    @Test
    void shouldNotBeSettled_untilAPaymentIsConfirmed() {
        when(settings.applicationFee()).thenReturn(new BigDecimal("300"));

        assertThat(guard.isSettled(41L, "APPLICATION_FEE")).isFalse();

        when(mapper.countConfirmed(41L, "APPLICATION_FEE")).thenReturn(1);
        assertThat(guard.isSettled(41L, "APPLICATION_FEE")).isTrue();
    }

    @Test
    void shouldNeverSettleAnUnknownKind() {
        when(settings.applicationFee()).thenReturn(BigDecimal.ZERO);

        assertThat(guard.isSettled(41L, "SERVICE_FEE")).isFalse();
    }
}
