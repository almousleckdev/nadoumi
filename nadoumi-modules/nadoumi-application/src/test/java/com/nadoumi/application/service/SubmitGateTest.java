package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.application.exception.DocumentsRequiredException;
import com.nadoumi.application.exception.PaymentRequiredException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubmitGateTest {

    private static final long ID = 7L;

    private final PaymentGuardProvider payments = mock(PaymentGuardProvider.class);
    private final ApplicationChecklistService checklist = mock(ApplicationChecklistService.class);
    private final SubmitGate gate = new SubmitGate(payments, checklist);

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

        assertThatCode(() -> gate.require(ID)).doesNotThrowAnyException();
    }
}
