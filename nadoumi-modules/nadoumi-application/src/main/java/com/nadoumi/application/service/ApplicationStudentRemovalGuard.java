package com.nadoumi.application.service;

import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.student.StudentRemovalParticipant;
import org.springframework.stereotype.Component;

/** Applications are business records: a student who has any cannot be deleted, only suspended or blocked. */
@Component
class ApplicationStudentRemovalGuard implements StudentRemovalParticipant {

    private final ApplicationMapper applications;

    ApplicationStudentRemovalGuard(ApplicationMapper applications) {
        this.applications = applications;
    }

    @Override
    public void beforeRemove(Subject subject) {
        if (subject.applicantIds().isEmpty()) {
            return;
        }
        int count = applications.countByApplicantIds(subject.applicantIds());
        if (count > 0) {
            throw new NadBadRequestException("This student has " + count
                    + (count == 1 ? " application" : " applications")
                    + " on record and cannot be deleted. Suspend or block the account instead.");
        }
    }
}
