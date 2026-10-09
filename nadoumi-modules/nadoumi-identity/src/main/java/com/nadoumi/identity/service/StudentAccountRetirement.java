package com.nadoumi.identity.service;

import com.nadoumi.common.erasure.StudentRetirementParticipant;
import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes the student sign-in accounts left without any applicant once an applicant is erased. Chats, tickets and the
 * rest are removed first, so the account row can normally be deleted outright; if a foreign key still points at it
 * the row is kept as an anonymised shell. Either way the username and email are free for a new registration.
 */
@Service
public class StudentAccountRetirement {

    private static final Logger log = LoggerFactory.getLogger(StudentAccountRetirement.class);

    private final NadIdentityMapper identity;
    private final UserApplicantAccessMapper access;
    private final SessionRevoker sessions;

    private final ObjectProvider<StudentRetirementParticipant> participants;

    public StudentAccountRetirement(NadIdentityMapper identity, UserApplicantAccessMapper access, SessionRevoker sessions,
            ObjectProvider<StudentRetirementParticipant> participants) {
        this.identity = identity;
        this.access = access;
        this.sessions = sessions;
        this.participants = participants;
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
            if (identity.selectStudentEmail(userId) == null) {
                continue; // staff and already-closed accounts are never touched
            }
            participants.orderedStream().forEach(participant -> participant.retire(userId));
            sessions.revokeAll(userId, null);
            close(userId, actorId);
        }
    }

    /**
     * Removes the account row. When something else still points at the user (for example a grant they gave on
     * another applicant), the row is kept as an anonymised shell instead: no username, email, phone or photo is
     * left, and the identifiers are free for a new registration either way.
     */
    private void close(Long userId, long actorId) {
        try {
            identity.removeStudentRoles(userId);
            identity.removeStudentPosts(userId);
            if (identity.removeStudentRow(userId) > 0) {
                log.info("student account {} deleted by staff {}", userId, actorId);
                return;
            }
        }
        catch (DataIntegrityViolationException e) {
            log.info("student account {} is still referenced, keeping an anonymised shell", userId);
        }
        identity.softDeleteStudent(userId, String.valueOf(actorId));
        log.info("student account {} anonymised by staff {}", userId, actorId);
    }
}
