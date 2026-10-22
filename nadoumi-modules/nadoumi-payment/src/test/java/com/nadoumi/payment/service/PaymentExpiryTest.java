package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentExpiryTest {

    private static final Instant NOW_INSTANT = Instant.parse("2026-10-22T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);

    private final PaymentMapper mapper = mock(PaymentMapper.class);
    private final PaymentExpiry expiry = new PaymentExpiry(mapper, Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));

    @Test
    void shouldExpireEveryOverdueSession_andRecordEachOne() {
        when(mapper.findOverdueIds(NOW)).thenReturn(List.of(1L, 2L, 3L));
        when(mapper.markExpired(1L, NOW)).thenReturn(1);
        when(mapper.markExpired(2L, NOW)).thenReturn(0); // paid or cancelled in the meantime
        when(mapper.markExpired(3L, NOW)).thenReturn(1);

        assertThat(expiry.expireOverdue()).isEqualTo(2);

        verify(mapper).insertEvent(1L, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "SYSTEM", null, null, NOW);
        verify(mapper).insertEvent(3L, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "SYSTEM", null, null, NOW);
    }
}
