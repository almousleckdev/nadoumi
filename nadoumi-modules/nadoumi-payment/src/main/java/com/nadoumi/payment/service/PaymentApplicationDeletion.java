package com.nadoumi.payment.service;

import com.nadoumi.common.erasure.ApplicationDeletionParticipant;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.exception.NadConflictException;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import com.nadoumi.payment.mapper.PaymentMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * An application's part in being deleted: its payment attempts, proof screenshots and generated receipts go with it. A
 * confirmed payment blocks the deletion, because it was booked as revenue and the ledger must keep matching the applications.
 */
@Component
@Order(10)
class PaymentApplicationDeletion implements ApplicationDeletionParticipant {

    private static final long SYSTEM_ACTOR = 0L;

    private final PaymentMapper payments;
    private final PaymentDocumentMapper documents;
    private final MediaGateway media;

    PaymentApplicationDeletion(PaymentMapper payments, PaymentDocumentMapper documents, MediaGateway media) {
        this.payments = payments;
        this.documents = documents;
        this.media = media;
    }

    @Override
    public void assertDeletable(long applicationId) {
        if (payments.countConfirmedAny(applicationId) > 0) {
            throw new NadConflictException("this application has a confirmed payment, which is booked revenue and cannot be deleted");
        }
    }

    @Override
    public void deleteFor(long applicationId) {
        documents.findMediaIdsByApplication(applicationId).forEach(id -> media.softDelete(id, SYSTEM_ACTOR));
        documents.deleteByApplicationId(applicationId); // a document references its payment
        payments.findProofMediaIdsByApplication(applicationId).forEach(id -> media.softDelete(id, SYSTEM_ACTOR));
        payments.deleteByApplicationId(applicationId);
    }
}
