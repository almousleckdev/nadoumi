package com.nadoumi.support.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.support.domain.SupportTicket;
import com.nadoumi.support.domain.SupportTicketEvent;
import com.nadoumi.support.domain.enums.TicketCategory;
import com.nadoumi.support.domain.enums.TicketEventType;
import com.nadoumi.support.domain.enums.TicketPriority;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.mapper.SupportTicketEventMapper;
import com.nadoumi.support.mapper.SupportTicketMapper;
import com.nadoumi.support.mapper.SupportUserMapper;
import com.ruoyi.common.utils.AuditActor;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The single place a ticket's state changes. Every change (a) is validated, (b) updates
 * {@code nad_support_ticket}, (c) appends exactly one {@code nad_support_ticket_event} row
 * (state is never silently overwritten, CLAUDE.md §9) and (d) writes its outbox event, all
 * inside the caller's transaction. Both the student and the staff service go through here so
 * the rules cannot drift between audiences.
 *
 * <p>Status lifecycle: see {@link TicketStatus}. {@code CLOSED} is terminal -- a student who
 * needs more help opens a new ticket.</p>
 */
@Component
public class TicketWorkflow {

    /** Status changes the student is told about; every other transition is internal triage. */
    private static final Set<TicketStatus> STUDENT_VISIBLE = Set.of(TicketStatus.RESOLVED, TicketStatus.WAITING_ON_STUDENT);

    static final String AUDIENCE_VIEW_PERMISSION = "nad:support:ticket:view";
    private static final String AGGREGATE = "support_ticket";

    private final SupportTicketMapper tickets;
    private final SupportTicketEventMapper events;
    private final SupportUserMapper users;
    private final OutboxWriter outbox;

    public TicketWorkflow(SupportTicketMapper tickets, SupportTicketEventMapper events, SupportUserMapper users,
            OutboxWriter outbox) {
        this.tickets = tickets;
        this.events = events;
        this.users = users;
        this.outbox = outbox;
    }

    /** Persists a new OPEN ticket, its OPENED event, and the TicketOpened outbox event. */
    public void persistNew(SupportTicket ticket, long actorId) {
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.NORMAL);
        ticket.setCreateBy(AuditActor.username());
        tickets.insert(ticket);
        recordEvent(ticket.getId(), TicketEventType.OPENED, null, TicketStatus.OPEN.name(), actorId);

        JSONObject payload = basePayload(ticket);
        payload.put("category", ticket.getCategory().name());
        payload.put("openedByName", displayName(actorId));
        payload.put("audiencePermission", AUDIENCE_VIEW_PERMISSION);
        outbox.write(AGGREGATE, ticket.getId(), OutboxEventTypes.TICKET_OPENED, payload.toJSONString());
    }

    public void changeStatus(SupportTicket ticket, TicketStatus next, long actorId) {
        TicketStatus current = ticket.getStatus();
        if (!current.canMoveTo(next)) {
            throw new NadBadRequestException("ticket cannot move from " + current + " to " + next);
        }
        if (tickets.updateStatus(ticket.getId(), current.name(), next.name(), AuditActor.username()) == 0) {
            throw new NadBadRequestException("ticket status changed concurrently, reload and retry");
        }
        ticket.setStatus(next);
        recordEvent(ticket.getId(), TicketEventType.STATUS_CHANGED, current.name(), next.name(), actorId);

        if (STUDENT_VISIBLE.contains(next)) {
            JSONObject payload = basePayload(ticket);
            payload.put("recipientUserIds", new JSONArray(new Object[] {ticket.getOpenedByUserId()}));
            payload.put("status", next.name());
            outbox.write(AGGREGATE, ticket.getId(), OutboxEventTypes.TICKET_STATUS_CHANGED, payload.toJSONString());
        }
    }

    /** Assigns or reassigns. The assignee must be a staff account; self-assignment does not notify. */
    public void assign(SupportTicket ticket, long assigneeId, long actorId) {
        if (!CurrentCaller.STAFF.equals(users.findUserType(assigneeId))) {
            throw new NadBadRequestException("assignee must be a staff user");
        }
        Long previous = ticket.getAssignedStaffId();
        if (previous != null && previous == assigneeId) {
            return;
        }
        tickets.updateAssignee(ticket.getId(), assigneeId, AuditActor.username());
        ticket.setAssignedStaffId(assigneeId);
        recordEvent(ticket.getId(), previous == null ? TicketEventType.ASSIGNED : TicketEventType.REASSIGNED,
                previous == null ? null : String.valueOf(previous), String.valueOf(assigneeId), actorId);

        if (assigneeId != actorId) {
            JSONObject payload = basePayload(ticket);
            payload.put("recipientUserIds", new JSONArray(new Object[] {assigneeId}));
            payload.put("assignedByName", displayName(actorId));
            outbox.write(AGGREGATE, ticket.getId(), OutboxEventTypes.TICKET_ASSIGNED, payload.toJSONString());
        }
    }

    public void changePriority(SupportTicket ticket, TicketPriority next, long actorId) {
        TicketPriority current = ticket.getPriority();
        if (current == next) {
            return;
        }
        tickets.updatePriority(ticket.getId(), next.name(), AuditActor.username());
        ticket.setPriority(next);
        recordEvent(ticket.getId(), TicketEventType.PRIORITY_CHANGED, current.name(), next.name(), actorId);
    }

    public void changeCategory(SupportTicket ticket, TicketCategory next, long actorId) {
        TicketCategory current = ticket.getCategory();
        if (current == next) {
            return;
        }
        tickets.updateCategory(ticket.getId(), next.name(), AuditActor.username());
        ticket.setCategory(next);
        recordEvent(ticket.getId(), TicketEventType.CATEGORY_CHANGED, current.name(), next.name(), actorId);
    }

    private void recordEvent(long ticketId, TicketEventType type, String oldValue, String newValue, long actorId) {
        SupportTicketEvent event = new SupportTicketEvent();
        event.setTicketId(ticketId);
        event.setEventType(type);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setActorUserId(actorId);
        events.insert(event);
    }

    private JSONObject basePayload(SupportTicket ticket) {
        JSONObject payload = new JSONObject();
        payload.put("ticketId", ticket.getId());
        payload.put("conversationId", ticket.getConversationId());
        payload.put("subject", ticket.getSubject());
        return payload;
    }

    private String displayName(long userId) {
        String name = users.findDisplayName(userId);
        return name == null ? "Someone" : name;
    }
}
