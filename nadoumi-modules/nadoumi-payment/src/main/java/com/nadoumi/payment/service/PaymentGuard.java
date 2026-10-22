package com.nadoumi.payment.service;

import com.nadoumi.application.service.PaymentGuardProvider;
import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.mapper.PaymentMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the application module's submit gate: an application may be submitted when no fee is configured or
 * when its Nadoumi application fee has a CONFIRMED payment. An unknown kind is never settled (fail closed).
 */
@Component
public class PaymentGuard implements PaymentGuardProvider {

    private final PaymentMapper mapper;
    private final PaymentSettings settings;

    public PaymentGuard(PaymentMapper mapper, PaymentSettings settings) {
        this.mapper = mapper;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isSettled(long applicationId, String kind) {
        if (!APPLICATION_FEE.equals(kind)) {
            return false;
        }
        return settings.applicationFee().signum() <= 0 || mapper.countConfirmed(applicationId, kind) > 0;
    }
}
