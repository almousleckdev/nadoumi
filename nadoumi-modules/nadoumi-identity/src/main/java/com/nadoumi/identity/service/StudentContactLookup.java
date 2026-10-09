package com.nadoumi.identity.service;

import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.nadoumi.identity.domain.UserApplicantAccess;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds the student account that owns an applicant, for messages that belong to that person (the welcome email). */
@Service
public class StudentContactLookup {

    /** The owning student's account id and sign-in email. */
    public record StudentContact(long userId, String email) {
    }

    private final UserApplicantAccessMapper access;
    private final NadIdentityMapper identity;

    public StudentContactLookup(UserApplicantAccessMapper access, NadIdentityMapper identity) {
        this.access = access;
        this.identity = identity;
    }

    /** Empty when the applicant has no active student owner, for example one a staff member created. */
    @Transactional(readOnly = true)
    public Optional<StudentContact> ownerOf(long applicantId) {
        UserApplicantAccess owner = access.findActiveOwner(applicantId);
        if (owner == null || owner.getUserId() == null) {
            return Optional.empty();
        }
        String email = identity.selectStudentEmail(owner.getUserId());
        return email == null ? Optional.empty() : Optional.of(new StudentContact(owner.getUserId(), email));
    }
}
