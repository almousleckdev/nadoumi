package com.nadoumi.payment.service;

import com.nadoumi.common.erasure.ApplicantErasureParticipant;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.payment.mapper.PaymentDocumentMapper;
import com.nadoumi.payment.mapper.PaymentMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Removes an applicant's payment attempts and generated documents, and soft-deletes their proof screenshots and PDFs. It runs before the
 * application module's erasure (order 20) because a payment references its application. The booked revenue
 * row is a financial record and is deliberately kept; it holds only a reference, an amount and a date.
 */
@Component
@Order(10)
class PaymentErasure implements ApplicantErasureParticipant {

    private static final long SYSTEM_ACTOR = 0L;

    private final PaymentMapper mapper;
    private final PaymentDocumentMapper documents;
    private final MediaGateway media;

    PaymentErasure(PaymentMapper mapper, PaymentDocumentMapper documents, MediaGateway media) {
        this.mapper = mapper;
        this.documents = documents;
        this.media = media;
    }

    @Override
    public void erase(long applicantId) {
        documents.findMediaIdsByApplicant(applicantId).forEach(id -> media.softDelete(id, SYSTEM_ACTOR));
        documents.deleteByApplicantId(applicantId); // a document references its payment
        mapper.findProofMediaIdsByApplicant(applicantId).forEach(id -> media.softDelete(id, SYSTEM_ACTOR));
        mapper.deleteByApplicantId(applicantId);
    }
}
