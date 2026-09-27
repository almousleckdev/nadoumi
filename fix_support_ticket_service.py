import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/service/SupportTicketService.java"
with open(path, "r") as f:
    text = f.read()

# Fix meetingMapper
text = text.replace("private final OutboxWriter outbox;", "private final OutboxWriter outbox;\n    private final SupportMeetingMapper meetingMapper;")
# But wait, did I do this already and it failed? Let's use re.sub just in case.

text = re.sub(r'public SupportTicketService\([^\)]+\)\s*\{', 'public SupportTicketService(SupportTicketMapper tickets, ConversationService conversations, CurrentCaller caller, TicketWorkflow workflow, com.nadoumi.common.outbox.OutboxWriter outbox, SupportMeetingMapper meetingMapper) {', text)
text = re.sub(r'this\.outbox = outbox;', 'this.outbox = outbox;\n        this.meetingMapper = meetingMapper;', text)

# Fix PostMessageRequest
text = text.replace("com.nadoumi.communication.web.request.PostMessageRequest msg = new com.nadoumi.communication.web.request.PostMessageRequest();\n        msg.setBody(\"System: A \" + req.getDurationMinutes() + \"-minute meeting has been scheduled for \" + req.getStartTime() + \".\");", "com.nadoumi.communication.web.request.PostMessageRequest msg = new com.nadoumi.communication.web.request.PostMessageRequest(\"System: A \" + req.getDurationMinutes() + \"-minute meeting has been scheduled for \" + req.getStartTime() + \".\", java.util.List.of());")

with open(path, "w") as f:
    f.write(text)

