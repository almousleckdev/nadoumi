package com.nadoumi.identity.service;

import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes the student sign-in accounts left without any applicant once an applicant is erased. RuoYi never
 * hard-deletes users (chats, tickets and audit rows keep pointing at them), so the account is soft-deleted and
 * anonymised, which also frees its username and email for a new registration.
 */
@Service
public class StudentAccountRetirement {

    private static final Logger log = LoggerFactory.getLogger(StudentAccountRetirement.class);

    private final NadIdentityMapper identity;
    private final UserApplicantAccessMapper access;
    private final SessionRevoker sessions;

    public StudentAccountRetirement(NadIdentityMapper identity, UserApplicantAccessMapper access, SessionRevoker sessions) {
        this.identity = identity;
        this.access = access;
        this.sessions = sessions;
    }

    /** Users that hold (or held) a grant on the applicant; read before the grants are erased. */
    @Transactional(readOnly = true)
    public List<Long> usersOf(long applicantId) {
        return access.userIdsForApplicant(applicantId);
    }

    /** Erases the applicant's grants. Must run before the applications they point at are deleted. */
    @Transactional(rollbackFor = Exception.class)
    public void eraseGrants(long applicantId) {
        access.deleteByApplicantId(applicantId);
    }

    /** Retires each of these users that is a student and no longer has any applicant. Staff are never touched. */
    @Transactional(rollbackFor = Exception.class)
    public void retireOrphans(List<Long> userIds, long actorId) {
        for (Long userId : userIds) {
            if (access.countForUser(userId) > 0) {
                continue;
            }
            if (identity.softDeleteStudent(userId, String.valueOf(actorId)) > 0) {
                sessions.revokeAll(userId, null);
                log.info("student account {} retired by staff {}", userId, actorId);
            }
        }
    }
}
