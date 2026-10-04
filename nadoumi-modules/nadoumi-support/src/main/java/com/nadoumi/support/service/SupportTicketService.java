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
import com.nadoumi.support.domain.SupportMeeting;
import com.nadoumi.support.domain.SupportTicket;
import com.nadoumi.support.domain.enums.MeetingStatus;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.mapper.SupportMeetingMapper;
import com.nadoumi.support.mapper.SupportTicketMapper;
import com.nadoumi.support.web.request.BookMeetingRequest;
import com.nadoumi.support.web.request.CreateTicketRequest;
import com.nadoumi.support.web.response.StudentTicketDetail;
import com.nadoumi.support.web.response.StudentTicketSummary;
import com.ruoyi.common.utils.AuditActor;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final SupportMeetingMapper meetingMapper;

    public SupportTicketService(SupportTicketMapper tickets, ConversationService conversations, CurrentCaller caller, TicketWorkflow workflow, NadoumiAccessService access, SupportMeetingMapper meetingMapper) {
        this.tickets = tickets;
        this.workflow = workflow;
        this.conversations = conversations;
        this.caller = caller;
        this.access = access;
        this.meetingMapper = meetingMapper;
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
                conversations.listMessages(ticket.getConversationId(), 0).stream().sorted(Comparator.comparing(MessageResponse::createdAt)).toList());
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

    
    @Transactional(rollbackFor = Exception.class)
    public void close(long id) {
        SupportTicket ticket = findOwned(id);
        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            workflow.changeStatus(ticket, TicketStatus.CLOSED, caller.requireUserId());
        }
    }

    
    @Transactional(rollbackFor = Exception.class)
    public SupportMeeting bookMeeting(long ticketId, BookMeetingRequest req) {
        SupportTicket ticket = findOwned(ticketId);
        if (ticket.getAssignedStaffId() == null) {
            throw new NadBadRequestException("Ticket must be assigned to a staff member before booking a meeting");
        }
        if (req.getStartTime().isBefore(LocalDateTime.now())) {
            throw new NadBadRequestException("Cannot book a meeting in the past");
        }
        
        LocalDateTime end = req.getStartTime().plusMinutes(req.getDurationMinutes());
        int staffConflicts = meetingMapper.countOverlappingStaffMeetings(ticket.getAssignedStaffId(), req.getStartTime(), end);
        if (staffConflicts > 0) {
            throw new NadBadRequestException("The assigned staff member is busy at that time");
        }
        int studentConflicts = meetingMapper.countOverlappingStudentMeetings(ticket.getApplicantId(), req.getStartTime(), end);
        if (studentConflicts > 0) {
            throw new NadBadRequestException("You already have a meeting scheduled at that time");
        }
        
        SupportMeeting meeting = new SupportMeeting();
        meeting.setTicketId(ticket.getId());
        meeting.setStudentId(ticket.getApplicantId());
        meeting.setStaffId(ticket.getAssignedStaffId());
        meeting.setStartTime(req.getStartTime());
        meeting.setDurationMinutes(req.getDurationMinutes());
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setCreateBy(AuditActor.username());
        meetingMapper.insert(meeting);
        
        PostMessageRequest msg = new PostMessageRequest("System: A " + req.getDurationMinutes() + "-minute meeting has been scheduled for " + req.getStartTime() + ".", List.of());
        conversations.post(ticket.getConversationId(), msg);
        
        return meeting;
    }

    private SupportTicket findOwned(long id) {
        SupportTicket ticket = tickets.findById(id);
        if (ticket == null || !ticket.getOpenedByUserId().equals(caller.requireUserId())) {
            throw new NadNotFoundException("ticket not found");
        }
        return ticket;
    }
}
