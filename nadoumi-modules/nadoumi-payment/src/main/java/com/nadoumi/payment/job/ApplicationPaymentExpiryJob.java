package com.nadoumi.payment.job;

import com.nadoumi.payment.service.PaymentExpiry;
import com.ruoyi.common.core.job.SchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Quartz job (bean name {@code applicationPaymentExpiryJob}, seeded by V111 as
 * {@code applicationPaymentExpiryJob.run()}). Student-facing reads also expire a session lazily, so this job
 * only keeps the staff queue and the history tidy.
 */
@Component("applicationPaymentExpiryJob")
public class ApplicationPaymentExpiryJob implements SchedulableJob {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPaymentExpiryJob.class);

    private final PaymentExpiry expiry;

    public ApplicationPaymentExpiryJob(PaymentExpiry expiry) {
        this.expiry = expiry;
    }

    @Override
    public void run() {
        int expired = expiry.expireOverdue();
        if (expired > 0) {
            log.info("payment sessions expired: {}", expired);
        }
    }
}
