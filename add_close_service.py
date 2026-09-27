import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/service/SupportTicketService.java"
with open(path, "r") as f:
    text = f.read()

script_add = """
    @Transactional(rollbackFor = Exception.class)
    public void close(long id) {
        SupportTicket ticket = findOwned(id);
        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            workflow.changeStatus(ticket, TicketStatus.CLOSED, caller.requireUserId());
        }
    }
"""

text = text.replace("private SupportTicket findOwned(long id) {", script_add + "\n    private SupportTicket findOwned(long id) {")

with open(path, "w") as f:
    f.write(text)

