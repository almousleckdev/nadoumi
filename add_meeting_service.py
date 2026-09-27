import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/service/SupportTicketService.java"
with open(path, "r") as f:
    text = f.read()

script_add = """
    @Transactional(rollbackFor = Exception.class)
    public SupportMeeting bookMeeting(long ticketId, com.nadoumi.support.web.request.BookMeetingRequest req) {
        SupportTicket ticket = findOwned(ticketId);
        if (ticket.getAssignedStaffId() == null) {
            throw new com.nadoumi.common.exception.NadBadRequestException("Ticket must be assigned to a staff member before booking a meeting");
        }
        if (req.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            throw new com.nadoumi.common.exception.NadBadRequestException("Cannot book a meeting in the past");
        }
        
        java.time.LocalDateTime end = req.getStartTime().plusMinutes(req.getDurationMinutes());
        int staffConflicts = meetingMapper.countOverlappingStaffMeetings(ticket.getAssignedStaffId(), req.getStartTime(), end);
        if (staffConflicts > 0) {
            throw new com.nadoumi.common.exception.NadBadRequestException("The assigned staff member is busy at that time");
        }
        int studentConflicts = meetingMapper.countOverlappingStudentMeetings(ticket.getApplicantId(), req.getStartTime(), end);
        if (studentConflicts > 0) {
            throw new com.nadoumi.common.exception.NadBadRequestException("You already have a meeting scheduled at that time");
        }
        
        SupportMeeting meeting = new SupportMeeting();
        meeting.setTicketId(ticket.getId());
        meeting.setStudentId(ticket.getApplicantId());
        meeting.setStaffId(ticket.getAssignedStaffId());
        meeting.setStartTime(req.getStartTime());
        meeting.setDurationMinutes(req.getDurationMinutes());
        meeting.setStatus(com.nadoumi.support.domain.enums.MeetingStatus.SCHEDULED);
        meeting.setCreateBy(com.ruoyi.common.utils.AuditActor.username());
        meetingMapper.insert(meeting);
        
        com.nadoumi.communication.web.request.PostMessageRequest msg = new com.nadoumi.communication.web.request.PostMessageRequest();
        msg.setBody("System: A " + req.getDurationMinutes() + "-minute meeting has been scheduled for " + req.getStartTime() + ".");
        conversations.post(ticket.getConversationId(), msg);
        
        return meeting;
    }
"""

text = text.replace("import com.nadoumi.support.domain.SupportTicket;", "import com.nadoumi.support.domain.SupportTicket;\nimport com.nadoumi.support.domain.SupportMeeting;\nimport com.nadoumi.support.mapper.SupportMeetingMapper;")
text = text.replace("private final OutboxWriter outbox;", "private final OutboxWriter outbox;\n    private final SupportMeetingMapper meetingMapper;")
text = text.replace("OutboxWriter outbox) {", "OutboxWriter outbox, SupportMeetingMapper meetingMapper) {")
text = text.replace("this.outbox = outbox;", "this.outbox = outbox;\n        this.meetingMapper = meetingMapper;")

text = text.replace("private SupportTicket findOwned(long id) {", script_add + "\n    private SupportTicket findOwned(long id) {")

with open(path, "w") as f:
    f.write(text)

