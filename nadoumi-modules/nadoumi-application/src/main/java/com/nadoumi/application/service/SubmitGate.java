package com.nadoumi.application.service;

import com.nadoumi.applicant.service.SignatureService;
import com.nadoumi.applicant.service.SubmissionReadinessService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.exception.DocumentsRequiredException;
import com.nadoumi.application.exception.PaymentRequiredException;
import com.nadoumi.application.exception.ProfileIncompleteException;
import com.nadoumi.application.exception.SignatureRequiredException;
import com.nadoumi.application.mapper.ApplicationMapper;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * What must be true before an application leaves DRAFT: the Nadoumi application fee is settled, every mandatory
 * document is uploaded and the applicant has signed the form (it is not valid without the signature). {@link WorkflowService} runs it for every {@code submit} transition whoever asks (a student,
 * or staff acting for one), so no endpoint can submit around it.
 */
@Component
public class SubmitGate {

    private final PaymentGuardProvider payments;
    private final ApplicationChecklistService checklist;
    private final ApplicationMapper applications;
    private final SignatureService signatures;
    private final SubmissionReadinessService readiness;

    public SubmitGate(PaymentGuardProvider payments, ApplicationChecklistService checklist,
            ApplicationMapper applications, SignatureService signatures, SubmissionReadinessService readiness) {
        this.payments = payments;
        this.checklist = checklist;
        this.applications = applications;
        this.signatures = signatures;
        this.readiness = readiness;
    }

    public void require(long applicationId) {
        if (!payments.isSettled(applicationId, PaymentGuardProvider.APPLICATION_FEE)) {
            throw new PaymentRequiredException("the application fee must be paid before the application can be submitted");
        }
        List<String> missing = checklist.missingMandatory(applicationId);
        if (!missing.isEmpty()) {
            throw new DocumentsRequiredException("upload the required documents before submitting: " + String.join(", ", missing));
        }
        Application application = applications.findById(applicationId);
        if (application != null) {
            List<String> unfinished = readiness.missing(application.getApplicantId());
            if (!unfinished.isEmpty()) {
                throw new ProfileIncompleteException(
                        "finish the applicant's profile before submitting: " + String.join(", ", unfinished));
            }
        }
        if (application == null || !signatures.isOnFile(application.getApplicantId())) {
            throw new SignatureRequiredException("sign the application form before submitting");
        }
    }
}
