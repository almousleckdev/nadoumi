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

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.service.ConversationService;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.support.domain.SupportTicket;
import com.nadoumi.support.domain.enums.TicketCategory;
import com.nadoumi.support.domain.enums.TicketPriority;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.mapper.SupportTicketEventMapper;
import com.nadoumi.support.mapper.SupportTicketMapper;
import com.ruoyi.framework.web.service.PermissionService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class StaffSupportTicketServiceTest {

    private static final long ME = 10L;
    private static final long OTHER_STAFF = 11L;

    private final SupportTicketMapper tickets = mock(SupportTicketMapper.class);
    private final SupportTicketEventMapper events = mock(SupportTicketEventMapper.class);
    private final TicketWorkflow workflow = mock(TicketWorkflow.class);
    private final ConversationService conversations = mock(ConversationService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PermissionService rbac = mock(PermissionService.class);
    private final StaffSupportTicketService service =
            new StaffSupportTicketService(tickets, events, workflow, conversations, caller, rbac);

    private void asStaffWith(String... permissions) {
        when(caller.isStaff()).thenReturn(true);
        when(caller.requireUserId()).thenReturn(ME);
        for (String permission : permissions) {
            when(rbac.hasPermi(permission)).thenReturn(true);
        }
    }

    private static SupportTicket ticket(TicketStatus status, Long assignee) {
        SupportTicket t = new SupportTicket();
        t.setId(7L);
        t.setConversationId(9L);
        t.setOpenedByUserId(1L);
        t.setSubject("Visa question");
        t.setCategory(TicketCategory.APPLICATION);
        t.setPriority(TicketPriority.NORMAL);
        t.setStatus(status);
        t.setAssignedStaffId(assignee);
        return t;
    }

    private static MessageResponse message() {
        return new MessageResponse(1, 9L, ME, "Agent", "hi", LocalDateTime.now(), null, List.of());
    }

    // ---- required security tests: permission gates ----

    @Test
    void shouldRejectStatusChange_whenStaffLacksManagePermission() {
        asStaffWith("nad:support:ticket:view");

        assertThatThrownBy(() -> service.changeStatus(7L, TicketStatus.IN_PROGRESS))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflow, never()).changeStatus(any(), any(), anyLong());
    }

    @Test
    void shouldRejectAssign_whenStaffLacksAssignPermission() {
        asStaffWith("nad:support:ticket:view", "nad:support:ticket:manage");

        assertThatThrownBy(() -> service.assign(7L, OTHER_STAFF)).isInstanceOf(AccessDeniedException.class);
        verify(workflow, never()).assign(any(), anyLong(), anyLong());
        verify(conversations, never()).addParticipant(anyLong(), anyLong(), any());
    }

    @Test
    void shouldRejectEveryMethod_whenCallerIsNotStaffEvenWithThePermissionString() {
        when(caller.isStaff()).thenReturn(false);
        when(rbac.hasPermi(any())).thenReturn(true);

        assertThatThrownBy(() -> service.list(null, null, null, null, 0, 20)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.reply(7L, new PostMessageRequest("x", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRejectQueue_whenStaffLacksViewPermission() {
        asStaffWith();

        assertThatThrownBy(() -> service.list(null, null, null, null, 0, 20)).isInstanceOf(AccessDeniedException.class);
        verify(tickets, never()).listForStaff(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    // ---- pool model ----

    @Test
    void shouldAllowAnyManagerToAct_whenTicketIsUnassigned() {
        asStaffWith("nad:support:ticket:manage");
        SupportTicket t = ticket(TicketStatus.OPEN, null);
        when(tickets.findById(7L)).thenReturn(t);

        service.changeStatus(7L, TicketStatus.IN_PROGRESS);

        verify(workflow).changeStatus(t, TicketStatus.IN_PROGRESS, ME);
    }

    @Test
    void shouldRejectManager_whenTicketIsAssignedToSomeoneElse() {
        asStaffWith("nad:support:ticket:manage");
        when(tickets.findById(7L)).thenReturn(ticket(TicketStatus.IN_PROGRESS, OTHER_STAFF));

        assertThatThrownBy(() -> service.changeStatus(7L, TicketStatus.RESOLVED))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflow, never()).changeStatus(any(), any(), anyLong());
    }

    @Test
    void shouldAllowAssigneeAndSupervisor_whenTicketIsAssigned() {
        asStaffWith("nad:support:ticket:manage");
        SupportTicket mine = ticket(TicketStatus.IN_PROGRESS, ME);
        when(tickets.findById(7L)).thenReturn(mine);
        service.changePriority(7L, TicketPriority.HIGH);
        verify(workflow).changePriority(mine, TicketPriority.HIGH, ME);

        when(rbac.hasPermi("nad:support:ticket:assign")).thenReturn(true);
        SupportTicket theirs = ticket(TicketStatus.IN_PROGRESS, OTHER_STAFF);
        when(tickets.findById(8L)).thenReturn(theirs);
        service.changeCategory(8L, TicketCategory.PAYMENT);
        verify(workflow).changeCategory(theirs, TicketCategory.PAYMENT, ME);
    }

    @Test
    void shouldAssignAndAddAssigneeAsStaffParticipant_whenSupervisorAssigns() {
        asStaffWith("nad:support:ticket:assign");
        SupportTicket t = ticket(TicketStatus.OPEN, null);
        when(tickets.findById(7L)).thenReturn(t);

        service.assign(7L, OTHER_STAFF);

        verify(workflow).assign(t, OTHER_STAFF, ME);
        verify(conversations).addParticipant(9L, OTHER_STAFF, ParticipantRole.STAFF);
    }

    @Test
    void shouldJoinClaimAndStartWork_whenFirstStaffRepliesToAnOpenUnassignedTicket() {
        asStaffWith("nad:support:ticket:manage");
        SupportTicket t = ticket(TicketStatus.OPEN, null);
        when(tickets.findById(7L)).thenReturn(t);
        when(conversations.post(eq(9L), any())).thenReturn(message());

        service.reply(7L, new PostMessageRequest("looking into it", null));

        verify(conversations).addParticipant(9L, ME, ParticipantRole.STAFF);
        verify(workflow).assign(t, ME, ME);
        verify(workflow).changeStatus(t, TicketStatus.IN_PROGRESS, ME);
    }

    @Test
    void shouldNotClaimOrChangeStatus_whenAssigneeRepliesToAnInProgressTicket() {
        asStaffWith("nad:support:ticket:manage");
        when(tickets.findById(7L)).thenReturn(ticket(TicketStatus.IN_PROGRESS, ME));
        when(conversations.post(eq(9L), any())).thenReturn(message());

        service.reply(7L, new PostMessageRequest("update", null));

        verify(workflow, never()).assign(any(), anyLong(), anyLong());
        verify(workflow, never()).changeStatus(any(), any(), anyLong());
    }

    @Test
    void shouldRejectReply_whenTicketIsClosed() {
        asStaffWith("nad:support:ticket:manage");
        when(tickets.findById(7L)).thenReturn(ticket(TicketStatus.CLOSED, ME));

        assertThatThrownBy(() -> service.reply(7L, new PostMessageRequest("late", null)))
                .isInstanceOf(NadBadRequestException.class);
        verify(conversations, never()).post(anyLong(), any());
    }

    @Test
    void shouldReturnNotFound_whenStaffOpensAMissingTicket() {
        asStaffWith("nad:support:ticket:view");

        assertThatThrownBy(() -> service.get(404L)).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void shouldReadThreadWithoutJoining_whenStaffViewsTicketDetail() {
        asStaffWith("nad:support:ticket:view");
        when(tickets.findById(7L)).thenReturn(ticket(TicketStatus.OPEN, null));
        when(conversations.listSupportMessagesForStaff(9L, 0)).thenReturn(List.of(message()));

        var detail = service.get(7L);

        assertThat(detail.messages()).hasSize(1);
        verify(conversations, never()).addParticipant(anyLong(), anyLong(), any());
    }
}
