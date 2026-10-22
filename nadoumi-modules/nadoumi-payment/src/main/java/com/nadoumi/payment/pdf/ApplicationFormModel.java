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
        ApplicantDossier dossier,
        /** A small JPEG of the applicant, or null when there is no usable photo. */
        byte[] photo,
        /** The applicant's signature as PNG bytes and the day it was given; both null when unsigned. */
        byte[] signature,
        java.time.LocalDate signedOn) {
}
