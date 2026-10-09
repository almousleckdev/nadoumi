package com.nadoumi.applicant.service;

import com.nadoumi.common.student.StudentRemovalParticipant;
import org.springframework.stereotype.Component;

/** When a student account is deleted, the applicant profiles it owned are archived, not erased. */
@Component
class ApplicantStudentRemoval implements StudentRemovalParticipant {

    private final ApplicantService applicants;

    ApplicantStudentRemoval(ApplicantService applicants) {
        this.applicants = applicants;
    }

    @Override
    public void afterRemove(Subject subject) {
        subject.ownedIds().forEach(applicants::archive);
    }
}
