package com.nadoumi.communication.service;

import com.nadoumi.common.access.AccessRole;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ConversationGuard {

    private final ConversationMapper conversations;
    private final ConversationParticipantMapper participants;
    private final CurrentCaller caller;
    private final UserApplicantAccessMapper grants;

    public ConversationGuard(ConversationMapper conversations, ConversationParticipantMapper participants,
            CurrentCaller caller, UserApplicantAccessMapper grants) {
        this.conversations = conversations;
        this.participants = participants;
        this.caller = caller;
        this.grants = grants;
    }

    public void requireActiveParticipant(long conversationId, long userId) {
        if (conversations.findById(conversationId) == null) {
            throw new NadNotFoundException("conversation not found");
        }
        if (participants.findActive(conversationId, userId) == null) {
            throw new AccessDeniedException("not a participant of conversation " + conversationId);
        }
    }

    public void requireStaff() {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("staff only");
        }
    }

    /** OWNER maps to APPLICANT (the applicant themselves); AGENT/GUARDIAN pass through. Staff never reach here. */
    public ParticipantRole participantRoleFor(long applicantId) {
        if (caller.isStaff()) {
            return ParticipantRole.STAFF;
        }
        UserApplicantAccess grant = grants.findActiveApplicantGrant(caller.requireUserId(), applicantId);
        AccessRole role = grant == null ? AccessRole.OWNER : grant.getAccessRole();
        return switch (role) {
            case AGENT -> ParticipantRole.AGENT;
            case GUARDIAN -> ParticipantRole.GUARDIAN;
            case OWNER, VIEWER -> ParticipantRole.APPLICANT;
        };
    }
}
