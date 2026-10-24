package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.service.ApplicantDossierService;
import com.nadoumi.application.service.ApplicationOpportunityService;
import com.nadoumi.application.service.ApplicationOpportunityService.Description;
import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class VerifiedDocumentDetailsTest {

    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final ApplicationOpportunityService opportunities = mock(ApplicationOpportunityService.class);
    private final ApplicantDossierService dossiers = mock(ApplicantDossierService.class);
    private final VerifiedDocumentDetails details = new VerifiedDocumentDetails(payments, opportunities, dossiers);

    private static PaymentDocument document() {
        PaymentDocument d = new PaymentDocument();
        d.setPaymentId(5L);
        d.setApplicationId(9L);
        d.setGeneratedAt(LocalDateTime.of(2026, 10, 22, 6, 31));
        return d;
    }

    @Test
    void shouldShowNameProgrammeAndFeeLine_andNothingElse() {
        ApplicationPayment payment = new ApplicationPayment();
        payment.setApplicantId(3L);
        payment.setAmount(new BigDecimal("300"));
        payment.setCurrency("CNY");
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setVerifiedAt(LocalDateTime.of(2026, 10, 22, 6, 30));
        when(payments.findById(5L)).thenReturn(payment);
        var dossier = mock(com.nadoumi.applicant.service.ApplicantDossier.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(dossier.profile().givenName()).thenReturn("Amina");
        when(dossier.profile().familyName()).thenReturn("Diallo");
        when(dossiers.read(3L)).thenReturn(dossier);
        when(opportunities.describe(9L)).thenReturn(new Description("NAD2026007", null, "MBBS", "Fudan University", "AUTUMN_SEPTEMBER"));

        var shown = details.of(document()).orElseThrow();

        assertThat(shown.applicantName()).isEqualTo("Amina Diallo");
        assertThat(shown.applicationReference()).isEqualTo("NAD2026007");
        assertThat(shown.opportunity()).isEqualTo("MBBS");
        assertThat(shown.feeStatus()).isEqualTo("CONFIRMED");
        assertThat(shown.feePaidAt()).isEqualTo("2026-10-22T06:30:00Z");
        assertThat(shown.issuedAt()).isEqualTo("2026-10-22T06:31:00Z");
    }

    @Test
    void shouldShowNothing_whenThePaymentIsGone() {
        when(payments.findById(5L)).thenReturn(null);

        assertThat(details.of(document())).isEmpty();
    }
}
