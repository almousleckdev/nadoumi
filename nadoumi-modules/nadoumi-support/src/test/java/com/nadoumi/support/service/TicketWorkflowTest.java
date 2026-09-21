package com.nadoumi.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.support.domain.SupportTicket;
import com.nadoumi.support.domain.SupportTicketEvent;
import com.nadoumi.support.domain.enums.TicketCategory;
import com.nadoumi.support.domain.enums.TicketEventType;
import com.nadoumi.support.domain.enums.TicketPriority;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.mapper.SupportTicketEventMapper;
import com.nadoumi.support.mapper.SupportTicketMapper;
import com.nadoumi.support.mapper.SupportUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TicketWorkflowTest {

    private static final long STUDENT = 1L;
    private static final long STAFF_A = 10L;
    private static final long STAFF_B = 11L;

    private final SupportTicketMapper tickets = mock(SupportTicketMapper.class);
    private final SupportTicketEventMapper events = mock(SupportTicketEventMapper.class);
    private final SupportUserMapper users = mock(SupportUserMapper.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final TicketWorkflow workflow = new TicketWorkflow(tickets, events, users, outbox);

    private static SupportTicket ticket(TicketStatus status) {
        SupportTicket t = new SupportTicket();
        t.setId(7L);
        t.setConversationId(9L);
        t.setOpenedByUserId(STUDENT);
        t.setSubject("Visa question");
        t.setCategory(TicketCategory.APPLICATION);
        t.setPriority(TicketPriority.NORMAL);
        t.setStatus(status);
        return t;
    }

    private SupportTicketEvent onlyEvent() {
        ArgumentCaptor<SupportTicketEvent> captor = ArgumentCaptor.forClass(SupportTicketEvent.class);
        verify(events, times(1)).insert(captor.capture());
        return captor.getValue();
    }

    private JSONObject payloadOf(String eventType) {
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outbox).write(eq("support_ticket"), eq(7L), eq(eventType), payload.capture());
        return JSON.parseObject(payload.getValue());
    }

    @Test
    void shouldWriteOpenedEventAndTicketOpenedOutbox_whenPersistingNewTicket() {
        doAnswer(inv -> {
            inv.<SupportTicket>getArgument(0).setId(7L);
            return 1;
        }).when(tickets).insert(any());
        when(users.findDisplayName(STUDENT)).thenReturn("Ada");
        SupportTicket t = ticket(null);
        t.setId(null);

        workflow.persistNew(t, STUDENT);

        assertThat(t.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(t.getPriority()).isEqualTo(TicketPriority.NORMAL);
        SupportTicketEvent event = onlyEvent();
        assertThat(event.getEventType()).isEqualTo(TicketEventType.OPENED);
        assertThat(event.getActorUserId()).isEqualTo(STUDENT);
        JSONObject payload = payloadOf(OutboxEventTypes.TICKET_OPENED);
        assertThat(payload.getLongValue("ticketId")).isEqualTo(7L);
        assertThat(payload.getString("subject")).isEqualTo("Visa question");
        assertThat(payload.getString("category")).isEqualTo("APPLICATION");
        assertThat(payload.getString("openedByName")).isEqualTo("Ada");
        assertThat(payload.getString("audiencePermission")).isEqualTo("nad:support:ticket:view");
        assertThat(payload.containsKey("recipientUserIds")).isFalse();
    }

    @Test
    void shouldWriteOneEventAndNotifyOpener_whenTicketIsResolved() {
        when(tickets.updateStatus(7L, "IN_PROGRESS", "RESOLVED", "system")).thenReturn(1);
        SupportTicket t = ticket(TicketStatus.IN_PROGRESS);

        workflow.changeStatus(t, TicketStatus.RESOLVED, STAFF_A);

        SupportTicketEvent event = onlyEvent();
        assertThat(event.getEventType()).isEqualTo(TicketEventType.STATUS_CHANGED);
        assertThat(event.getOldValue()).isEqualTo("IN_PROGRESS");
        assertThat(event.getNewValue()).isEqualTo("RESOLVED");
        assertThat(t.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        JSONObject payload = payloadOf(OutboxEventTypes.TICKET_STATUS_CHANGED);
        assertThat(payload.getJSONArray("recipientUserIds")).containsExactly((int) STUDENT);
        assertThat(payload.getString("status")).isEqualTo("RESOLVED");
    }

    @Test
    void shouldNotNotifyStudent_whenTransitionIsInternalTriage() {
        when(tickets.updateStatus(7L, "OPEN", "IN_PROGRESS", "system")).thenReturn(1);

        workflow.changeStatus(ticket(TicketStatus.OPEN), TicketStatus.IN_PROGRESS, STAFF_A);

        onlyEvent();
        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void shouldRejectAndWriteNothing_whenTransitionIsNotInTheMatrix() {
        assertThatThrownBy(() -> workflow.changeStatus(ticket(TicketStatus.OPEN), TicketStatus.CLOSED, STAFF_A))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("OPEN to CLOSED");
        assertThatThrownBy(() -> workflow.changeStatus(ticket(TicketStatus.CLOSED), TicketStatus.OPEN, STAFF_A))
                .isInstanceOf(NadBadRequestException.class);

        verify(tickets, never()).updateStatus(anyLong(), anyString(), anyString(), anyString());
        verify(events, never()).insert(any());
        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void shouldRejectAndWriteNothing_whenStatusChangedConcurrently() {
        when(tickets.updateStatus(7L, "OPEN", "IN_PROGRESS", "system")).thenReturn(0);

        assertThatThrownBy(() -> workflow.changeStatus(ticket(TicketStatus.OPEN), TicketStatus.IN_PROGRESS, STAFF_A))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("concurrently");

        verify(events, never()).insert(any());
    }

    @Test
    void shouldWriteAssignedEventAndNotifyAssignee_whenAssigningAnUnassignedTicket() {
        when(users.findUserType(STAFF_B)).thenReturn("00");
        when(users.findDisplayName(STAFF_A)).thenReturn("Manager");
        SupportTicket t = ticket(TicketStatus.OPEN);

        workflow.assign(t, STAFF_B, STAFF_A);

        SupportTicketEvent event = onlyEvent();
        assertThat(event.getEventType()).isEqualTo(TicketEventType.ASSIGNED);
        assertThat(event.getOldValue()).isNull();
        assertThat(event.getNewValue()).isEqualTo("11");
        verify(tickets).updateAssignee(7L, STAFF_B, "system");
        JSONObject payload = payloadOf(OutboxEventTypes.TICKET_ASSIGNED);
        assertThat(payload.getJSONArray("recipientUserIds")).containsExactly((int) STAFF_B);
        assertThat(payload.getString("assignedByName")).isEqualTo("Manager");
    }

    @Test
    void shouldWriteReassignedEventWithOldAssignee_whenReassigning() {
        when(users.findUserType(STAFF_B)).thenReturn("00");
        SupportTicket t = ticket(TicketStatus.IN_PROGRESS);
        t.setAssignedStaffId(STAFF_A);

        workflow.assign(t, STAFF_B, 99L);

        SupportTicketEvent event = onlyEvent();
        assertThat(event.getEventType()).isEqualTo(TicketEventType.REASSIGNED);
        assertThat(event.getOldValue()).isEqualTo("10");
        assertThat(event.getNewValue()).isEqualTo("11");
    }

    @Test
    void shouldNotNotify_whenStaffClaimsATicketForThemselves() {
        when(users.findUserType(STAFF_A)).thenReturn("00");

        workflow.assign(ticket(TicketStatus.OPEN), STAFF_A, STAFF_A);

        onlyEvent();
        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void shouldRejectAssignee_whenTargetIsNotAStaffAccount() {
        when(users.findUserType(STUDENT)).thenReturn("10");

        assertThatThrownBy(() -> workflow.assign(ticket(TicketStatus.OPEN), STUDENT, STAFF_A))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("staff");

        verify(events, never()).insert(any());
    }

    @Test
    void shouldDoNothing_whenAssigningToTheCurrentAssignee() {
        when(users.findUserType(STAFF_A)).thenReturn("00");
        SupportTicket t = ticket(TicketStatus.IN_PROGRESS);
        t.setAssignedStaffId(STAFF_A);

        workflow.assign(t, STAFF_A, STAFF_B);

        verify(events, never()).insert(any());
        verify(tickets, never()).updateAssignee(anyLong(), any(), anyString());
    }

    @Test
    void shouldWriteOneEventEach_whenPriorityAndCategoryChange() {
        SupportTicket t = ticket(TicketStatus.IN_PROGRESS);

        workflow.changePriority(t, TicketPriority.URGENT, STAFF_A);
        workflow.changeCategory(t, TicketCategory.PAYMENT, STAFF_A);

        ArgumentCaptor<SupportTicketEvent> captor = ArgumentCaptor.forClass(SupportTicketEvent.class);
        verify(events, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SupportTicketEvent::getEventType)
                .containsExactly(TicketEventType.PRIORITY_CHANGED, TicketEventType.CATEGORY_CHANGED);
        assertThat(captor.getAllValues().get(0).getOldValue()).isEqualTo("NORMAL");
        assertThat(captor.getAllValues().get(0).getNewValue()).isEqualTo("URGENT");
        assertThat(captor.getAllValues().get(1).getOldValue()).isEqualTo("APPLICATION");
        assertThat(captor.getAllValues().get(1).getNewValue()).isEqualTo("PAYMENT");
    }

    @Test
    void shouldWriteNoEvent_whenPriorityOrCategoryIsUnchanged() {
        SupportTicket t = ticket(TicketStatus.IN_PROGRESS);

        workflow.changePriority(t, TicketPriority.NORMAL, STAFF_A);
        workflow.changeCategory(t, TicketCategory.APPLICATION, STAFF_A);

        verify(events, never()).insert(any());
    }
}
