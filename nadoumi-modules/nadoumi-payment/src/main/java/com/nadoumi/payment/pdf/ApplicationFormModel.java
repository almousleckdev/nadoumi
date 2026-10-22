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
        ApplicantDossier dossier) {
}
