package com.nadoumi.payment.service;

import com.nadoumi.application.service.ApplicationReadService;
import com.nadoumi.application.service.PaymentGuardProvider;
import java.math.BigDecimal;
import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.mapper.PaymentMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the application module's gates: an application may be submitted when no application fee is configured or
 * when it has a CONFIRMED payment, and the admission letter is released on the same terms for the service fee.
 * An unknown kind is never settled (fail closed).
 */
@Component
public class PaymentGuard implements PaymentGuardProvider {

    private final PaymentMapper mapper;
    private final PaymentSettings settings;
    private final ApplicationReadService applications;

    public PaymentGuard(PaymentMapper mapper, PaymentSettings settings, ApplicationReadService applications) {
        this.mapper = mapper;
        this.settings = settings;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isSettled(long applicationId, String kind) {
        if (!isKnown(kind)) {
            return false;
        }
        return !feeRequired(applicationId, kind) || mapper.countConfirmed(applicationId, kind) > 0;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isPaidOrReported(long applicationId, String kind) {
        if (!isKnown(kind)) {
            return false;
        }
        return !feeRequired(applicationId, kind) || mapper.countPaidOrReported(applicationId, kind) > 0;
    }

    private static boolean isKnown(String kind) {
        return APPLICATION_FEE.equals(kind) || SERVICE_FEE.equals(kind);
    }

    /** No fee configured for the kind means nothing is asked, so nothing is unsettled. */
    private boolean feeRequired(long applicationId, String kind) {
        BigDecimal fee = APPLICATION_FEE.equals(kind) ? settings.applicationFee() : applications.serviceFee(applicationId);
        return fee.signum() > 0;
    }
}
