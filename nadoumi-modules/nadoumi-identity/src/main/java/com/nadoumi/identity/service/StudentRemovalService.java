package com.nadoumi.identity.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.student.StudentRemovalParticipant;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff deletion of a student account. The account is soft-deleted and anonymised (RuoYi never hard-deletes
 * users, and chats, tickets and audit rows keep pointing at it), every session ends, and every access grant is
 * revoked. Other modules veto or clean up through {@link StudentRemovalParticipant}: a student who has
 * applications cannot be deleted, because those are business records (and later payments) that must be kept.
 */
@Service
public class StudentRemovalService {

    private static final Logger log = LoggerFactory.getLogger(StudentRemovalService.class);
    private static final String STUDENT_USER_TYPE = "10";

    private final NadIdentityMapper identity;
    private final UserApplicantAccessMapper access;
    private final SessionRevoker sessions;
    private final CurrentCaller caller;
    private final ObjectProvider<StudentRemovalParticipant> participants;

    public StudentRemovalService(NadIdentityMapper identity, UserApplicantAccessMapper access, SessionRevoker sessions,
            CurrentCaller caller, ObjectProvider<StudentRemovalParticipant> participants) {
        this.identity = identity;
        this.access = access;
        this.sessions = sessions;
        this.caller = caller;
        this.participants = participants;
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(long userId) {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("deleting a student is a staff action");
        }
        long staffId = caller.requireUserId();
        if (!STUDENT_USER_TYPE.equals(identity.selectUserType(userId))) {
            throw new NadNotFoundException("student not found");
        }
        List<Long> applicantIds = access.accessibleApplicantIds(userId);
        StudentRemovalParticipant.Subject subject =
                new StudentRemovalParticipant.Subject(userId, applicantIds, access.ownedApplicantIds(userId));

        participants.orderedStream().forEach(p -> p.beforeRemove(subject));

        if (identity.softDeleteStudent(userId, String.valueOf(staffId)) == 0) {
            throw new NadBadRequestException("this student account is already deleted");
        }
        access.revokeAllForUser(userId, staffId, String.valueOf(staffId));
        participants.orderedStream().forEach(p -> p.afterRemove(subject));
        sessions.revokeAll(userId, null);
        log.info("student account {} deleted by staff {}", userId, staffId);
    }
}
