package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.enums.PersonRole;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.mapper.FamilyMapper;
import com.nadoumi.applicant.rules.FamilyRules;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What an applicant must have finished before any application for them is submitted, whoever submits it (the
 * student, a user acting for them, or staff). No caller check: the submit gate decides, so this is not exposed to
 * the web. The signature is checked separately by the gate.
 */
@Service
public class SubmissionReadinessService {

    public static final String ONBOARDING = "ONBOARDING";

    private final ApplicantMapper applicants;
    private final FamilyMapper family;

    public SubmissionReadinessService(ApplicantMapper applicants, FamilyMapper family) {
        this.applicants = applicants;
        this.family = family;
    }

    /** The unfinished sections, in the order the student meets them; empty when the applicant is ready. */
    @Transactional(readOnly = true)
    public List<String> missing(long applicantId) {
        List<String> missing = new ArrayList<>();
        Applicant applicant = applicants.findById(applicantId);
        if (applicant == null || applicant.getOnboardedAt() == null) {
            missing.add(ONBOARDING);
        }
        if (!FormReadinessService.personalComplete(family.findPersonal(applicantId))) {
            missing.add(FormReadinessService.PERSONAL_DETAILS);
        }
        if (!FamilyRules.isComplete(family.findMembers(applicantId))) {
            missing.add(FormReadinessService.FAMILY);
        }
        if (family.findPerson(applicantId, PersonRole.SUPPORTER) == null) {
            missing.add(FormReadinessService.SUPPORTER);
        }
        if (family.findPerson(applicantId, PersonRole.CHINA_EMERGENCY) == null) {
            missing.add(FormReadinessService.CHINA_EMERGENCY);
        }
        return List.copyOf(missing);
    }
}
