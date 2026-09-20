package com.nadoumi.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.communication.service.ConversationService;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.support.domain.SupportTicket;
import com.nadoumi.support.domain.enums.TicketCategory;
import com.nadoumi.support.domain.enums.TicketPriority;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.mapper.SupportTicketMapper;
import com.nadoumi.support.web.request.CreateTicketRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SupportTicketServiceTest {

    private static final long ME = 1L;
    private static final long SOMEONE_ELSE = 2L;

    private final SupportTicketMapper tickets = mock(SupportTicketMapper.class);
    private final TicketWorkflow workflow = mock(TicketWorkflow.class);
    private final ConversationService conversations = mock(ConversationService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final SupportTicketService service = new SupportTicketService(tickets, workflow, conversations, caller, access);

    private static SupportTicket ticket(long openedBy, TicketStatus status) {
        SupportTicket t = new SupportTicket();
        t.setId(7L);
        t.setConversationId(9L);
        t.setOpenedByUserId(openedBy);
        t.setSubject("Visa question");
        t.setCategory(TicketCategory.APPLICATION);
        t.setPriority(TicketPriority.URGENT);
        t.setStatus(status);
        t.setAssignedStaffId(55L);
        return t;
    }

    private static MessageResponse message(long id) {
        return new MessageResponse(id, 9L, ME, "Ada", "hello", LocalDateTime.now(), null, List.of());
    }

    // ---- required security test: a student cannot see or reply to another student's ticket ----

    @Test
    void shouldReturnNotFound_whenStudentReadsAnotherStudentsTicket() {
        when(caller.requireUserId()).thenReturn(SOMEONE_ELSE);
        when(tickets.findById(7L)).thenReturn(ticket(ME, TicketStatus.OPEN));

        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(NadNotFoundException.class);
        verify(conversations, never()).listMessages(anyLong(), anyLong());
    }

    @Test
    void shouldReturnNotFound_whenStudentRepliesToAnotherStudentsTicket() {
        when(caller.requireUserId()).thenReturn(SOMEONE_ELSE);
        when(tickets.findById(7L)).thenReturn(ticket(ME, TicketStatus.OPEN));

        assertThatThrownBy(() -> service.reply(7L, new PostMessageRequest("hi", null)))
                .isInstanceOf(NadNotFoundException.class);
        verify(conversations, never()).post(anyLong(), any());
    }

    @Test
    void shouldReturnNotFound_whenTicketDoesNotExist() {
        when(caller.requireUserId()).thenReturn(ME);

        assertThatThrownBy(() -> service.get(404L)).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void shouldOnlyQueryTheCallersOwnTickets_whenListing() {
        when(caller.requireUserId()).thenReturn(ME);
        when(tickets.listByOpener(ME, "OPEN", 0, 20)).thenReturn(List.of(ticket(ME, TicketStatus.OPEN)));

        var out = service.listMine(TicketStatus.OPEN, 0, 20);

        assertThat(out).hasSize(1);
        verify(tickets).listByOpener(ME, "OPEN", 0, 20);
    }

    @Test
    void shouldClampPaging_whenSizeIsOversizedOrNegative() {
        when(caller.requireUserId()).thenReturn(ME);

        service.listMine(null, -3, 100_000);
        service.listMine(null, 2, 0);

        verify(tickets).listByOpener(ME, null, 0, 100);
        verify(tickets).listByOpener(ME, null, 40, 20);
    }

    @Test
    void shouldNotLeakInternalTriageFields_whenBuildingTheStudentView() {
        when(caller.requireUserId()).thenReturn(ME);
        when(tickets.findById(7L)).thenReturn(ticket(ME, TicketStatus.IN_PROGRESS));
        when(conversations.listMessages(9L, 0)).thenReturn(List.of(message(1)));

        var detail = service.get(7L);

        assertThat(detail.ticket().status()).isEqualTo("IN_PROGRESS");
        assertThat(detail.conversationId()).isEqualTo(9L);
        assertThat(detail.messages()).hasSize(1);
        assertThat(java.util.Arrays.stream(detail.ticket().getClass().getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("priority", "assignedStaffId", "openedByUserId");
    }

    @Test
    void shouldCreateConversationThenTicket_whenOpeningWithoutAnApplicant() {
        when(caller.requireUserId()).thenReturn(ME);
        when(conversations.openSupport("Visa question", "help me")).thenReturn(message(100));
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<SupportTicket>getArgument(0).setId(7L);
            inv.<SupportTicket>getArgument(0).setStatus(TicketStatus.OPEN);
            inv.<SupportTicket>getArgument(0).setPriority(TicketPriority.NORMAL);
            return null;
        }).when(workflow).persistNew(any(), anyLong());

        var detail = service.create(new CreateTicketRequest("Visa question", TicketCategory.APPLICATION, "help me", null));

        ArgumentCaptor<SupportTicket> saved = ArgumentCaptor.forClass(SupportTicket.class);
        verify(workflow).persistNew(saved.capture(), eq(ME));
        assertThat(saved.getValue().getConversationId()).isEqualTo(9L);
        assertThat(saved.getValue().getOpenedByUserId()).isEqualTo(ME);
        assertThat(saved.getValue().getApplicantId()).isNull();
        assertThat(detail.conversationId()).isEqualTo(9L);
        verify(access, never()).canAccessApplicant(anyLong(), any());
    }

    @Test
    void shouldRejectCreate_whenApplicantIsGivenButCallerLacksMessageStaff() {
        when(caller.requireUserId()).thenReturn(ME);
        when(access.canAccessApplicant(5L, ApplicantCapability.MESSAGE_STAFF.name())).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateTicketRequest("s", TicketCategory.OTHER, "b", 5L)))
                .isInstanceOf(NadForbiddenException.class);
        verify(conversations, never()).openSupport(any(), any());
        verify(workflow, never()).persistNew(any(), anyLong());
    }

    @Test
    void shouldMoveBackToInProgress_whenStudentRepliesToATicketWaitingOnThem() {
        when(caller.requireUserId()).thenReturn(ME);
        SupportTicket t = ticket(ME, TicketStatus.WAITING_ON_STUDENT);
        when(tickets.findById(7L)).thenReturn(t);
        when(conversations.post(eq(9L), any())).thenReturn(message(101));

        service.reply(7L, new PostMessageRequest("here you go", null));

        verify(workflow).changeStatus(t, TicketStatus.IN_PROGRESS, ME);
    }

    @Test
    void shouldNotChangeStatus_whenStudentRepliesToAnInProgressTicket() {
        when(caller.requireUserId()).thenReturn(ME);
        when(tickets.findById(7L)).thenReturn(ticket(ME, TicketStatus.IN_PROGRESS));
        when(conversations.post(eq(9L), any())).thenReturn(message(101));

        service.reply(7L, new PostMessageRequest("more info", null));

        verify(workflow, never()).changeStatus(any(), any(), anyLong());
    }

    @Test
    void shouldRejectReply_whenTicketIsResolvedOrClosed() {
        when(caller.requireUserId()).thenReturn(ME);
        for (TicketStatus done : List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED)) {
            when(tickets.findById(7L)).thenReturn(ticket(ME, done));
            assertThatThrownBy(() -> service.reply(7L, new PostMessageRequest("again", null)))
                    .isInstanceOf(NadBadRequestException.class);
        }
        verify(conversations, never()).post(anyLong(), any());
    }
}
