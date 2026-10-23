package com.nadoumi.payment.job;

import com.nadoumi.payment.service.PaymentDocumentWorker;
import com.ruoyi.common.core.job.SchedulableJob;
import org.springframework.stereotype.Component;

/**
 * Quartz job (bean name {@code applicationPaymentDocumentsJob}, seeded by V113 as
 * {@code applicationPaymentDocumentsJob.run()}): renders and emails the receipt and the application form.
 */
@Component("applicationPaymentDocumentsJob")
public class ApplicationPaymentDocumentsJob implements SchedulableJob {

    private final PaymentDocumentWorker worker;

    public ApplicationPaymentDocumentsJob(PaymentDocumentWorker worker) {
        this.worker = worker;
    }

    @Override
    public void run() {
        worker.runOnce();
    }
}
