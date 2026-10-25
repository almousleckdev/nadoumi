package com.nadoumi.payment.service;

import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.service.ReceivablesSource;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tells finance how much reported application-fee money still waits for staff to confirm it. */
@Component
class PaymentReceivables implements ReceivablesSource {

    private final PaymentMapper payments;

    PaymentReceivables(PaymentMapper payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    @Override
    public List<MoneyTotal> awaitingConfirmation() {
        return payments.awaitingConfirmationTotals();
    }
}
