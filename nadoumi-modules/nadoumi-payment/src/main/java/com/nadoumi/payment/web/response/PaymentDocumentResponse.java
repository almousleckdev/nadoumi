package com.nadoumi.payment.web.response;

import com.nadoumi.payment.domain.PaymentDocument;

/** A generated document the student can download. No media id or storage URL. */
public record PaymentDocumentResponse(String kind, String number, int version, String generatedAt, int sizeBytes) {

    public static PaymentDocumentResponse of(PaymentDocument d) {
        return new PaymentDocumentResponse(d.getKind().name(), d.getDocNumber(), d.getVersion(),
                d.getGeneratedAt().toString(), d.getByteSize());
    }
}
