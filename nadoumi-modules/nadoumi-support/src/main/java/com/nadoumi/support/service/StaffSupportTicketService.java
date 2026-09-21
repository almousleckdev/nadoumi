package com.nadoumi.support.service;

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
import com.nadoumi.support.web.response.StaffTicketDetail;
import com.nadoumi.support.web.response.StaffTicketSummary;
import com.nadoumi.support.web.response.TicketEventResponse;
import com.ruoyi.framework.web.service.PermissionService;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff side of support tickets, a pool model (design doc §10 #2): any staff holding
 * {@code nad:support:ticket:manage} may work an unassigned ticket; once assigned, only the
 * assignee or a holder of {@code nad:support:ticket:assign} may act on it. The controllers'
 * {@code @PreAuthorize} is the first gate; every method re-checks here (defense in depth,
 * {@code security.md}).
 */
@Service
public class StaffSupportTicketService {

    static final String PERM_VIEW = "nad:support:ticket:view";
    static final String PERM_MANAGE = "nad:support:ticket:manage";
    static final String PERM_ASSIGN = "nad:support:ticket:assign";

    private final SupportTicketMapper tickets;
    private final SupportTicketEventMapper events;
    private final TicketWorkflow workflow;
    private final ConversationService conversations;
    private final CurrentCaller caller;
    private final PermissionService rbac;

    public StaffSupportTicketService(SupportTicketMapper tickets, SupportTicketEventMapper events,
            TicketWorkflow workflow, ConversationService conversations, CurrentCaller caller,
            PermissionService rbac) {
        this.tickets = tickets;
        this.events = events;
        this.workflow = workflow;
        this.conversations = conversations;
        this.caller = caller;
        this.rbac = rbac;
    }

    @Transactional(readOnly = true)
    public List<StaffTicketSummary> list(TicketStatus status, TicketPriority priority, TicketCategory category,
            Long assigneeId, int page, int size) {
        requirePermission(PERM_VIEW);
        return tickets.listForStaff(name(status), name(priority), name(category), assigneeId,
                        TicketPaging.offset(page, size), TicketPaging.limit(size)).stream()
                .map(StaffTicketSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StaffTicketDetail get(long id) {
        requirePermission(PERM_VIEW);
        SupportTicket ticket = find(id);
        return new StaffTicketDetail(StaffTicketSummary.from(ticket), ticket.getConversationId(),
                conversations.listSupportMessagesForStaff(ticket.getConversationId(), 0),
                events.listByTicket(id).stream().map(TicketEventResponse::from).toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffTicketSummary changeStatus(long id, TicketStatus next) {
        SupportTicket ticket = findForManagement(id);
        workflow.changeStatus(ticket, next, caller.requireUserId());
        return StaffTicketSummary.from(ticket);
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffTicketSummary assign(long id, long assigneeUserId) {
        requirePermission(PERM_ASSIGN);
        SupportTicket ticket = find(id);
        workflow.assign(ticket, assigneeUserId, caller.requireUserId());
        // the assignee must be able to see and post in the thread, and gets realtime pings
        conversations.addParticipant(ticket.getConversationId(), assigneeUserId, ParticipantRole.STAFF);
        return StaffTicketSummary.from(ticket);
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffTicketSummary changePriority(long id, TicketPriority priority) {
        SupportTicket ticket = findForManagement(id);
        workflow.changePriority(ticket, priority, caller.requireUserId());
        return StaffTicketSummary.from(ticket);
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffTicketSummary changeCategory(long id, TicketCategory category) {
        SupportTicket ticket = findForManagement(id);
        workflow.changeCategory(ticket, category, caller.requireUserId());
        return StaffTicketSummary.from(ticket);
    }

    /**
     * Staff reply. Joins the thread, and on an unassigned ticket the replier claims it; the first
     * reply on an OPEN ticket moves it to IN_PROGRESS.
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse reply(long id, PostMessageRequest req) {
        SupportTicket ticket = findForManagement(id);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new NadBadRequestException("ticket is closed");
        }
        long userId = caller.requireUserId();
        conversations.addParticipant(ticket.getConversationId(), userId, ParticipantRole.STAFF);
        MessageResponse posted = conversations.post(ticket.getConversationId(), req);
        if (ticket.getAssignedStaffId() == null) {
            workflow.assign(ticket, userId, userId);
        }
        if (ticket.getStatus() == TicketStatus.OPEN) {
            workflow.changeStatus(ticket, TicketStatus.IN_PROGRESS, userId);
        }
        return posted;
    }

    private SupportTicket find(long id) {
        SupportTicket ticket = tickets.findById(id);
        if (ticket == null) {
            throw new NadNotFoundException("ticket not found");
        }
        return ticket;
    }

    private SupportTicket findForManagement(long id) {
        requirePermission(PERM_MANAGE);
        SupportTicket ticket = find(id);
        Long assignee = ticket.getAssignedStaffId();
        boolean mayAct = assignee == null || assignee.equals(caller.requireUserId()) || rbac.hasPermi(PERM_ASSIGN);
        if (!mayAct) {
            throw new AccessDeniedException("ticket is assigned to another staff member");
        }
        return ticket;
    }

    private void requirePermission(String permission) {
        if (!caller.isStaff() || !rbac.hasPermi(permission)) {
            throw new AccessDeniedException("missing " + permission);
        }
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
