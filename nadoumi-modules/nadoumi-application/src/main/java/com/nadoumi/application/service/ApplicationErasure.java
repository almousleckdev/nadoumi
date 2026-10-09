package com.nadoumi.application.service;

import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.erasure.ApplicantErasureParticipant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Removes an applicant's applications; their events, tasks, decisions, snapshots and workflow state cascade. */
@Component
@Order(20)
class ApplicationErasure implements ApplicantErasureParticipant {

    private final ApplicationMapper applications;

    ApplicationErasure(ApplicationMapper applications) {
        this.applications = applications;
    }

    @Override
    public void erase(long applicantId) {
        applications.removeByApplicantId(applicantId);
    }
}
