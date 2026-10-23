package com.nadoumi.payment.service;

import com.nadoumi.applicant.service.ApplicantService;
import org.springframework.stereotype.Component;

/** The applicant's display name, for the staff payment queue. */
@Component
public class ApplicantNames {

    private final ApplicantService applicants;

    public ApplicantNames(ApplicantService applicants) {
        this.applicants = applicants;
    }

    public String of(long applicantId) {
        var applicant = applicants.get(applicantId);
        return (applicant.givenName() + " " + applicant.familyName()).strip();
    }
}
