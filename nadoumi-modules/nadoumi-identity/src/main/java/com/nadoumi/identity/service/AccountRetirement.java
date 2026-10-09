package com.nadoumi.identity.service;

import com.nadoumi.common.erasure.AccountRetirementParticipant;
import com.nadoumi.common.exception.NadBadRequestException;
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
 * Closes accounts for good: student logins left without any applicant once an applicant is erased, and staff
 * accounts an administrator deletes. Chats, tickets and the
 * rest are removed first, so the account row can normally be deleted outright; if a foreign key still points at it
 * the row is kept as an anonymised shell. Either way the username and email are free for a new registration.
 */
@Service
public class AccountRetirement {

    private static final Logger log = LoggerFactory.getLogger(AccountRetirement.class);
    private static final String STUDENT = "10";
    private static final String STAFF = "00";

    private final NadIdentityMapper identity;
    private final UserApplicantAccessMapper access;
    private final SessionRevoker sessions;

    private final ObjectProvider<AccountRetirementParticipant> participants;

    public AccountRetirement(NadIdentityMapper identity, UserApplicantAccessMapper access, SessionRevoker sessions,
            ObjectProvider<AccountRetirementParticipant> participants) {
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
            retire(userId, actorId, STUDENT);
        }
    }

    /**
     * Deletes a staff account completely: their chats, notifications and likes go first, then the account row
     * (an anonymised shell only if a record such as a payroll entry still references it). The caller has already
     * checked that this account may be deleted at all.
     */
    @Transactional(rollbackFor = Exception.class)
    public void retireStaff(long userId, long actorId) {
        if (!STAFF.equals(identity.selectUserType(userId))) {
            throw new NadBadRequestException("not a staff account");
        }
        retire(userId, actorId, STAFF);
    }

    private void retire(long userId, long actorId, String userType) {
        participants.orderedStream().forEach(participant -> participant.retire(userId));
        sessions.revokeAll(userId, null);
        close(userId, actorId, userType);
    }

    /**
     * Removes the account row. When something else still points at the user, the row is kept as an anonymised
     * shell instead: no username, email, phone or photo is left, and the identifiers are free for a new
     * registration either way.
     */
    private void close(long userId, long actorId, String userType) {
        try {
            identity.removeAccountRoles(userId);
            identity.removeAccountPosts(userId);
            if (identity.removeAccountRow(userId, userType) > 0) {
                log.info("account {} ({}) deleted by {}", userId, userType, actorId);
                return;
            }
        }
        catch (DataIntegrityViolationException e) {
            log.info("account {} is still referenced, keeping an anonymised shell", userId);
        }
        identity.anonymiseAccount(userId, userType, String.valueOf(actorId));
        log.info("account {} ({}) anonymised by {}", userId, userType, actorId);
    }
}
