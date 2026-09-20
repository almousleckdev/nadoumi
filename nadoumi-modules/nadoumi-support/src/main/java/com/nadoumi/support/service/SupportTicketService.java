package com.nadoumi.support.service;

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
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.mapper.SupportTicketMapper;
import com.nadoumi.support.web.request.CreateTicketRequest;
import com.nadoumi.support.web.response.StudentTicketDetail;
import com.nadoumi.support.web.response.StudentTicketSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student side of support tickets. A student only ever sees tickets they opened; anything
 * else is a 404 (not 403) so another student's ticket id is never confirmed to exist. Threading
 * is delegated to {@link ConversationService}, never its mappers.
 */
@Service
public class SupportTicketService {

    /** Statuses a student may still reply in. RESOLVED/CLOSED mean "done": open a new ticket. */
    private static final Set<TicketStatus> REPLIABLE =
            Set.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_ON_STUDENT);

    private final SupportTicketMapper tickets;
    private final TicketWorkflow workflow;
    private final ConversationService conversations;
    private final CurrentCaller caller;
    private final NadoumiAccessService access;

    public SupportTicketService(SupportTicketMapper tickets, TicketWorkflow workflow,
            ConversationService conversations, CurrentCaller caller, NadoumiAccessService access) {
        this.tickets = tickets;
        this.workflow = workflow;
        this.conversations = conversations;
        this.caller = caller;
        this.access = access;
    }

    /** Opens a ticket: creates the SUPPORT conversation + first message, then the ticket that owns it. */
    @Transactional(rollbackFor = Exception.class)
    public StudentTicketDetail create(CreateTicketRequest req) {
        long userId = caller.requireUserId();
        if (req.applicantId() != null
                && !access.canAccessApplicant(req.applicantId(), ApplicantCapability.MESSAGE_STAFF.name())) {
            throw new NadForbiddenException("missing MESSAGE_STAFF on applicant " + req.applicantId());
        }

        MessageResponse first = conversations.openSupport(req.subject(), req.body());

        SupportTicket ticket = new SupportTicket();
        ticket.setConversationId(first.conversationId());
        ticket.setApplicantId(req.applicantId());
        ticket.setOpenedByUserId(userId);
        ticket.setSubject(req.subject());
        ticket.setCategory(req.category());
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        workflow.persistNew(ticket, userId);

        return new StudentTicketDetail(StudentTicketSummary.from(ticket), ticket.getConversationId(), List.of(first));
    }

    @Transactional(readOnly = true)
    public List<StudentTicketSummary> listMine(TicketStatus status, int page, int size) {
        return tickets.listByOpener(caller.requireUserId(), status == null ? null : status.name(),
                        TicketPaging.offset(page, size), TicketPaging.limit(size)).stream()
                .map(StudentTicketSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentTicketDetail get(long id) {
        SupportTicket ticket = findOwned(id);
        return new StudentTicketDetail(StudentTicketSummary.from(ticket), ticket.getConversationId(),
                conversations.listMessages(ticket.getConversationId(), 0));
    }

    /** Reply on the caller's own ticket. A reply to a ticket waiting on the student moves it back to IN_PROGRESS. */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse reply(long id, PostMessageRequest req) {
        SupportTicket ticket = findOwned(id);
        if (!REPLIABLE.contains(ticket.getStatus())) {
            throw new NadBadRequestException("ticket is " + ticket.getStatus() + ", open a new ticket for further help");
        }
        MessageResponse posted = conversations.post(ticket.getConversationId(), req);
        if (ticket.getStatus() == TicketStatus.WAITING_ON_STUDENT) {
            workflow.changeStatus(ticket, TicketStatus.IN_PROGRESS, caller.requireUserId());
        }
        return posted;
    }

    private SupportTicket findOwned(long id) {
        SupportTicket ticket = tickets.findById(id);
        if (ticket == null || !ticket.getOpenedByUserId().equals(caller.requireUserId())) {
            throw new NadNotFoundException("ticket not found");
        }
        return ticket;
    }
}
