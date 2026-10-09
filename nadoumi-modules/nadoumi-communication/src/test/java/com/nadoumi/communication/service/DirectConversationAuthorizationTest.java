package com.nadoumi.communication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
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
import com.nadoumi.communication.web.request.StaffCreateConversationRequest;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** The privacy rules of chat: staff talk to students only, one private conversation per pair. */
class DirectConversationAuthorizationTest {

    private static final long STAFF_A = 1L;
    private static final long STAFF_B = 2L;
    private static final long STUDENT_A = 100L;
    private static final long STUDENT_B = 101L;

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
    private final ConversationGuard guard = new ConversationGuard(conversations, participants, caller, grants);
    private final com.nadoumi.communication.stream.ChatEvents events = mock(com.nadoumi.communication.stream.ChatEvents.class);
    private final ConversationService service = new ConversationService(conversations, participants, messages, users,
            access, caller, publisher, guard,
            new ConversationResponseAssembler(attachments, mock(com.nadoumi.identity.profile.PublicProfileService.class), media,
                    mock(com.nadoumi.communication.stream.PresenceService.class)),
            new ConversationAttachments(guard, attachments, messages, media, caller),
            events, attachments);

    DirectConversationAuthorizationTest() {
        when(users.isActiveStaff(STAFF_A)).thenReturn(true);
        when(users.isActiveStaff(STAFF_B)).thenReturn(true);
        when(users.isActiveStudent(STUDENT_A)).thenReturn(true);
        when(users.isActiveStudent(STUDENT_B)).thenReturn(true);
        when(conversations.insert(any())).thenAnswer(inv -> {
            ((Conversation) inv.getArgument(0)).setId(7L);
            return 1;
        });
        when(publisher.publish(anyLong(), anyLong(), any(), anyList())).thenAnswer(inv -> {
            Message m = new Message();
            m.setId(1L);
            m.setConversationId(inv.getArgument(0));
            m.setSenderUserId(inv.getArgument(1));
            m.setBody(inv.getArgument(2));
            return m;
        });
    }

    private void callerIs(long userId, boolean staff) {
        when(caller.requireUserId()).thenReturn(userId);
        when(caller.isStaff()).thenReturn(staff);
    }

    private static StaffCreateConversationRequest staffStarts(long studentUserId) {
        return new StaffCreateConversationRequest(studentUserId, null, "Hello", "Hi there");
    }

    @Test
    void shouldRejectStaffStartingAChatWithAnotherStaffMember_whenRecipientIsNotAStudent() {
        callerIs(STAFF_A, true);

        assertThatThrownBy(() -> service.createByStaff(staffStarts(STAFF_B)))
                .isInstanceOf(NadBadRequestException.class);

        verify(conversations, never()).insert(any());
    }

    @Test
    void shouldRejectStaffStartingAChat_whenCallerIsNotStaff() {
        callerIs(STUDENT_A, false);

        assertThatThrownBy(() -> service.createByStaff(staffStarts(STUDENT_B)))
                .isInstanceOf(AccessDeniedException.class);

        verify(conversations, never()).insert(any());
    }

    @Test
    void shouldCreateOnePrivateDirectChat_whenStaffMessagesAStudentForTheFirstTime() {
        callerIs(STAFF_A, true);

        service.createByStaff(staffStarts(STUDENT_A));

        verify(conversations).insert(org.mockito.ArgumentMatchers.argThat(c ->
                c.getConversationType() == ConversationType.DIRECT
                        && (STAFF_A + ":" + STUDENT_A).equals(c.getDirectKey())));
        verify(participants, org.mockito.Mockito.times(2)).insert(any());
    }

    @Test
    void shouldReuseTheExistingChat_whenTheSamePairStartsAgain() {
        callerIs(STAFF_A, true);
        Conversation existing = new Conversation();
        existing.setId(42L);
        existing.setStatus(ConversationStatus.OPEN);
        when(conversations.findByDirectKey(STAFF_A + ":" + STUDENT_A)).thenReturn(existing);

        service.createByStaff(staffStarts(STUDENT_A));

        verify(conversations, never()).insert(any());
        verify(publisher).publish(42L, STAFF_A, "Hi there", List.of());
    }

    @Test
    void shouldRejectAStudentOpeningAChatWithAnotherStudent_whenAdminIsNotStaff() {
        callerIs(STUDENT_A, false);

        assertThatThrownBy(() -> service.open(new OpenConversationRequest(null, null, STUDENT_B, null, "hi")))
                .isInstanceOf(NadBadRequestException.class);

        verify(conversations, never()).insert(any());
    }

    @Test
    void shouldOpenADirectChatWithTheChosenStaffMember_whenStudentPicksAnActiveAdvisor() {
        callerIs(STUDENT_A, false);

        service.open(new OpenConversationRequest(null, null, STAFF_B, null, "hi"));

        verify(conversations).insert(org.mockito.ArgumentMatchers.argThat(c ->
                (STAFF_B + ":" + STUDENT_A).equals(c.getDirectKey())));
    }

    @Test
    void shouldRejectAStaffMemberAddingThemselvesToAnotherStaffMembersChat() {
        callerIs(STAFF_B, true);
        Conversation direct = new Conversation();
        direct.setId(42L);
        direct.setConversationType(ConversationType.DIRECT);
        when(conversations.findById(42L)).thenReturn(direct);
        // STAFF_B is not a participant of conversation 42

        assertThatThrownBy(() -> service.addParticipant(42L, STAFF_B, ParticipantRole.STAFF))
                .isInstanceOf(AccessDeniedException.class);

        verify(participants, never()).insert(any());
    }

    @Test
    void shouldRejectAddingAStudentToADirectChat_whenAnActiveParticipantTriesIt() {
        callerIs(STAFF_A, true);
        Conversation direct = new Conversation();
        direct.setId(42L);
        direct.setConversationType(ConversationType.DIRECT);
        when(conversations.findById(42L)).thenReturn(direct);
        when(participants.findActive(42L, STAFF_A)).thenReturn(new ConversationParticipant());

        assertThatThrownBy(() -> service.addParticipant(42L, STUDENT_B, ParticipantRole.APPLICANT))
                .isInstanceOf(NadBadRequestException.class);

        verify(participants, never()).insert(any());
    }

    @Test
    void shouldRejectPosting_whenTheConversationIsClosed() {
        callerIs(STAFF_A, true);
        Conversation closed = new Conversation();
        closed.setId(42L);
        closed.setStatus(ConversationStatus.CLOSED);
        when(conversations.findById(42L)).thenReturn(closed);
        when(participants.findActive(42L, STAFF_A)).thenReturn(new ConversationParticipant());

        assertThatThrownBy(() -> service.post(42L, new PostMessageRequest("hello", null)))
                .isInstanceOf(NadBadRequestException.class);

        verify(publisher, never()).publish(anyLong(), anyLong(), any(), anyList());
        assertThat(closed.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    // ---- deleting a chat ----

    private Conversation direct(long id, ConversationType type) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setConversationType(type);
        c.setStatus(ConversationStatus.OPEN);
        when(conversations.findById(id)).thenReturn(c);
        return c;
    }

    @Test
    void shouldRejectDeletingAChat_whenTheStaffMemberIsNotInIt() {
        callerIs(STAFF_B, true);
        direct(42L, ConversationType.DIRECT);

        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(AccessDeniedException.class);

        verify(conversations, never()).deleteById(anyLong());
    }

    @Test
    void shouldRejectDeletingAChat_whenTheCallerIsAStudent() {
        callerIs(STUDENT_A, false);
        direct(42L, ConversationType.DIRECT);

        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(AccessDeniedException.class);

        verify(conversations, never()).deleteById(anyLong());
    }

    @Test
    void shouldRefuseDeletingATicketChat_becauseTheSupportModuleOwnsIt() {
        callerIs(STAFF_A, true);
        direct(42L, ConversationType.SUPPORT);
        when(participants.findActive(42L, STAFF_A)).thenReturn(new ConversationParticipant());

        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(NadBadRequestException.class);

        verify(conversations, never()).deleteById(anyLong());
    }

    @Test
    void shouldDeleteTheChatReleaseItsFilesAndTellTheStudent_whenAParticipantDeletesIt() {
        callerIs(STAFF_A, true);
        direct(42L, ConversationType.DIRECT);
        ConversationParticipant staff = new ConversationParticipant();
        staff.setUserId(STAFF_A);
        ConversationParticipant student = new ConversationParticipant();
        student.setUserId(STUDENT_A);
        when(participants.findActive(42L, STAFF_A)).thenReturn(staff);
        when(participants.listActiveForConversation(42L)).thenReturn(List.of(staff, student));
        when(attachments.listMediaIdsByConversation(42L)).thenReturn(List.of(501L, 502L));

        service.delete(42L);

        verify(conversations).deleteById(42L);
        verify(media).softDelete(501L, STAFF_A);
        verify(media).softDelete(502L, STAFF_A);
        verify(events).conversationRemoved(42L, STAFF_A, List.of(STAFF_A, STUDENT_A));
    }
}
