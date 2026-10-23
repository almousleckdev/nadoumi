package com.nadoumi.payment.service;

import com.nadoumi.application.service.ApplicationReadService;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * One pass of the documents job, in order: render the documents of newly confirmed payments, email the receipts,
 * render the final form for applications that have been submitted, email it to the administrators. Each item is
 * handled on its own, so one bad payment never blocks the rest, and every step is safe to repeat.
 */
@Component
public class PaymentDocumentWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentDocumentWorker.class);
    private static final int BATCH = 20;

    private final PaymentDocumentMapper documents;
    private final PaymentDocumentService service;
    private final PaymentDocumentDelivery delivery;
    private final ApplicationReadService applications;

    public PaymentDocumentWorker(PaymentDocumentMapper documents, PaymentDocumentService service,
            PaymentDocumentDelivery delivery, ApplicationReadService applications) {
        this.documents = documents;
        this.service = service;
        this.delivery = delivery;
        this.applications = applications;
    }

    public void runOnce() {
        for (ApplicationPayment payment : documents.findConfirmedWithoutReceipt(BATCH)) {
            attempt("render documents", payment, () -> service.generateForConfirmed(payment));
        }
        delivery.deliverReceipts();

        for (ApplicationPayment payment : documents.findAwaitingFinalForm(BATCH)) {
            boolean submitted = applications.find(payment.getApplicationId()).map(a -> !a.draft()).orElse(false);
            if (submitted) {
                attempt("render submitted form", payment, () -> service.generateSubmittedForm(payment));
            }
        }
        delivery.deliverSubmittedForms();
    }

    private void attempt(String what, ApplicationPayment payment, Runnable work) {
        try {
            work.run();
        }
        catch (RuntimeException e) {
            // ids only: the exception message can carry profile data
            log.warn("payment {}: could not {}; will retry", payment.getId(), what, e);
        }
    }
}
