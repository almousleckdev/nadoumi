package com.nadoumi.communication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.enums.ConversationStatus;
import com.nadoumi.communication.domain.enums.ConversationType;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.web.request.OpenConversationRequest;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ConversationServiceTest {

    private final ConversationMapper conversations = mock(ConversationMapper.class);
    private final ConversationParticipantMapper participants = mock(ConversationParticipantMapper.class);
    private final MessageMapper messages = mock(MessageMapper.class);
    private final MessageAttachmentMapper attachments = mock(MessageAttachmentMapper.class);
    private final CommunicationUserMapper users = mock(CommunicationUserMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final UserApplicantAccessMapper grants = mock(UserApplicantAccessMapper.class);
    private final MessagePublisher publisher = mock(MessagePublisher.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ConversationService service = new ConversationService(conversations, participants, messages,
            attachments, users, access, caller, grants, publisher, media);

    private static Conversation conversation() {
        Conversation c = new Conversation();
        c.setId(9L);
        c.setConversationType(ConversationType.GENERAL);
        c.setStatus(ConversationStatus.OPEN);
        return c;
    }

    // ---- required security test: a caller without MESSAGE_STAFF cannot open a conversation ----

    @Test
    void open_rejectsACallerWithoutMessageStaffOnTheApplicant() {
        when(access.canAccessApplicant(5L, ApplicantCapability.MESSAGE_STAFF.name())).thenReturn(false);
        var req = new OpenConversationRequest(5L, null, "Question", "Hi there");

        assertThatThrownBy(() -> service.open(req)).isInstanceOf(NadForbiddenException.class);
    }

    @Test
    void open_succeedsAndPublishesTheFirstMessageWhenAuthorized() {
        when(access.canAccessApplicant(5L, ApplicantCapability.MESSAGE_STAFF.name())).thenReturn(true);
        when(caller.requireUserId()).thenReturn(1L);
        when(caller.isStaff()).thenReturn(false);
        when(grants.findActiveApplicantGrant(1L, 5L)).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<Conversation>getArgument(0).setId(9L);
            return 1;
        }).when(conversations).insert(any());
        Message posted = new Message();
        posted.setId(100L);
        posted.setConversationId(9L);
        posted.setSenderUserId(1L);
        posted.setBody("Hi there");
        when(publisher.publish(anyLong(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("Hi there"), any())).thenReturn(posted);

        var req = new OpenConversationRequest(5L, null, "Question", "Hi there");
        service.open(req);

        verify(participants).insert(any());
        verify(publisher).publish(anyLong(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("Hi there"), any());
    }

    // ---- required security test: a non-participant cannot read or post to a conversation ----

    @Test
    void post_rejectsACallerWhoIsNotAnActiveParticipant() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 2L)).thenReturn(null);

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("hi", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(publisher, never()).publish(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void listMessages_rejectsACallerWhoIsNotAnActiveParticipant() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 2L)).thenReturn(null);

        assertThatThrownBy(() -> service.listMessages(9L, 0)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void post_rejectsWhenTheConversationDoesNotExist() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("hi", null)))
                .isInstanceOf(com.nadoumi.common.exception.NadNotFoundException.class);
    }

    // ---- required security test: staff-only transitions reject non-staff callers ----

    @Test
    void close_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.close(9L)).isInstanceOf(AccessDeniedException.class);
        verify(conversations, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void addParticipant_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.addParticipant(9L, 3L, ParticipantRole.STAFF))
                .isInstanceOf(AccessDeniedException.class);
        verify(participants, never()).insert(any());
    }

    @Test
    void removeParticipant_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.removeParticipant(9L, 3L)).isInstanceOf(AccessDeniedException.class);
        verify(participants, never()).remove(anyLong(), anyLong(), any());
    }

    @Test
    void listForStaff_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.listForStaff()).isInstanceOf(AccessDeniedException.class);
    }

    // ---- read-state math ----

    @Test
    void markRead_advancesToTheLatestMessageId() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        Message latest = new Message();
        latest.setId(55L);
        when(messages.findLatest(9L)).thenReturn(latest);

        service.markRead(9L);

        verify(participants).updateLastRead(9L, 1L, 55L);
    }

    @Test
    void markRead_isANoOpWhenTheConversationHasNoMessagesYet() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        when(messages.findLatest(9L)).thenReturn(null);

        service.markRead(9L);

        verify(participants, never()).updateLastRead(anyLong(), anyLong(), anyLong());
    }
}
