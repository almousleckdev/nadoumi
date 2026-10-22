package com.nadoumi.application.service;

import com.nadoumi.application.exception.DocumentsRequiredException;
import com.nadoumi.application.exception.PaymentRequiredException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * What must be true before an application leaves DRAFT: the Nadoumi application fee is settled and every mandatory
 * document is uploaded. {@link WorkflowService} runs it for every {@code submit} transition whoever asks (a student,
 * or staff acting for one), so no endpoint can submit around it.
 */
@Component
public class SubmitGate {

    private final PaymentGuardProvider payments;
    private final ApplicationChecklistService checklist;

    public SubmitGate(PaymentGuardProvider payments, ApplicationChecklistService checklist) {
        this.payments = payments;
        this.checklist = checklist;
    }

    public void require(long applicationId) {
        if (!payments.isSettled(applicationId, PaymentGuardProvider.APPLICATION_FEE)) {
            throw new PaymentRequiredException("the application fee must be paid before the application can be submitted");
        }
        List<String> missing = checklist.missingMandatory(applicationId);
        if (!missing.isEmpty()) {
            throw new DocumentsRequiredException("upload the required documents before submitting: " + String.join(", ", missing));
        }
    }
}
