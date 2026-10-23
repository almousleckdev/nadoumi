package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.service.SignatureService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.exception.DocumentsRequiredException;
import com.nadoumi.application.exception.PaymentRequiredException;
import com.nadoumi.application.exception.SignatureRequiredException;
import com.nadoumi.application.mapper.ApplicationMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubmitGateTest {

    private static final long ID = 7L;

    private final PaymentGuardProvider payments = mock(PaymentGuardProvider.class);
    private final ApplicationChecklistService checklist = mock(ApplicationChecklistService.class);
    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final SignatureService signatures = mock(SignatureService.class);
    private final SubmitGate gate = new SubmitGate(payments, checklist, applications, signatures);

    private void application() {
        Application a = new Application();
        a.setId(ID);
        a.setApplicantId(9L);
        when(applications.findById(ID)).thenReturn(a);
    }

    @Test
    void shouldRequirePaymentFirst() {
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(false);

        assertThatThrownBy(() -> gate.require(ID)).isInstanceOf(PaymentRequiredException.class);
    }

    @Test
    void shouldNameTheMissingDocuments() {
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(true);
        when(checklist.missingMandatory(ID)).thenReturn(List.of("PASSPORT", "TRANSCRIPT"));

        assertThatThrownBy(() -> gate.require(ID))
                .isInstanceOf(DocumentsRequiredException.class)
                .hasMessageContaining("PASSPORT").hasMessageContaining("TRANSCRIPT");
    }

    @Test
    void shouldLetItThrough_whenPaidAndDocumentsAreIn() {
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(true);
        when(checklist.missingMandatory(ID)).thenReturn(List.of());
        application();
        when(signatures.isOnFile(9L)).thenReturn(true);

        assertThatCode(() -> gate.require(ID)).doesNotThrowAnyException();
    }

    @Test
    void shouldRequireTheSignature_afterPaymentAndDocuments() {
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(true);
        when(checklist.missingMandatory(ID)).thenReturn(List.of());
        application();
        when(signatures.isOnFile(9L)).thenReturn(false);

        assertThatThrownBy(() -> gate.require(ID)).isInstanceOf(SignatureRequiredException.class);
    }
}
