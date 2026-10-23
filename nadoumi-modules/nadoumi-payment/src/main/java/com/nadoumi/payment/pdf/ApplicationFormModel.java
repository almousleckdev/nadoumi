package com.nadoumi.payment.pdf;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.application.service.ApplicationOpportunityService.Description;
import java.time.Instant;

/** What the printed application form needs. The university can only appear through {@code opportunity}. */
public record ApplicationFormModel(
        String formNo,
        Instant generatedAt,
        String applicationRef,
        Description opportunity,
        String paymentReference,
        /** The bank transaction number the student reported and when it was paid; null while unpaid. */
        String transactionNo,
        Instant paidAt,
        ApplicantDossier dossier,
        /** A small JPEG of the applicant, or null when there is no usable photo. */
        byte[] photo,
        /** The applicant's signature as PNG bytes and the day it was given; both null when unsigned. */
        byte[] signature,
        java.time.LocalDate signedOn,
        /** The address the printed QR code opens, or null for none. */
        String verifyUrl) {

    /** A form with no payment details beyond the reference (previews and fixtures). */
    public ApplicationFormModel(String formNo, Instant generatedAt, String applicationRef, Description opportunity,
            String paymentReference, ApplicantDossier dossier, byte[] photo, byte[] signature,
            java.time.LocalDate signedOn, String verifyUrl) {
        this(formNo, generatedAt, applicationRef, opportunity, paymentReference, null, null, dossier, photo, signature,
                signedOn, verifyUrl);
    }
}
