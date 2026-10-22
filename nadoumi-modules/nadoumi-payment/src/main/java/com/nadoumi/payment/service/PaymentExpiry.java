package com.nadoumi.payment.service;

import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Closes payment sessions whose 15-minute window ran out without a proof. */
@Service
public class PaymentExpiry {

    private final PaymentMapper mapper;
    private final Clock clock;

    public PaymentExpiry(PaymentMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** @return how many sessions were expired */
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> overdue = mapper.findOverdueIds(now);
        int expired = 0;
        for (Long id : overdue) {
            if (mapper.markExpired(id, now) == 1) {
                mapper.insertEvent(id, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "SYSTEM", null, null, now);
                expired++;
            }
        }
        return expired;
    }
}
