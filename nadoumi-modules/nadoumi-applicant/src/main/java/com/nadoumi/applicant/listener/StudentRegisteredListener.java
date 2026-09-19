package com.nadoumi.applicant.listener;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.identity.event.StudentRegisteredEvent;
import com.nadoumi.identity.service.UserApplicantAccessService;
import java.time.LocalDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives every new student a primary applicant profile at registration, in the same
 * transaction as the account, so onboarding starts with the name and the verified
 * email already filled in.
 */
@Component
public class StudentRegisteredListener {

    private final ApplicantMapper mapper;
    private final UserApplicantAccessService grants;

    public StudentRegisteredListener(ApplicantMapper mapper, UserApplicantAccessService grants) {
        this.mapper = mapper;
        this.grants = grants;
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onStudentRegistered(StudentRegisteredEvent event) {
        Applicant applicant = new Applicant();
        applicant.setGivenName(event.givenName());
        applicant.setFamilyName(event.familyName());
        applicant.setEmail(event.email());
        applicant.setEmailVerifiedAt(LocalDateTime.now());
        applicant.setStatus(ApplicantStatus.ACTIVE);
        applicant.setCreateBy(String.valueOf(event.userId()));
        mapper.insert(applicant);
        grants.grantOwnerOnSelfRegistration(event.userId(), applicant.getId());
    }
}
