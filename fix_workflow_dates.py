import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/service/TicketWorkflow.java"
with open(path, "r") as f:
    text = f.read()

script_add = """
        if (next == TicketStatus.RESOLVED) {
            tickets.updateResolvedAt(ticket.getId(), java.time.LocalDateTime.now(), AuditActor.username());
            ticket.setResolvedAt(java.time.LocalDateTime.now());
        }
        if (next == TicketStatus.CLOSED) {
            tickets.updateClosedAt(ticket.getId(), java.time.LocalDateTime.now(), AuditActor.username());
            ticket.setClosedAt(java.time.LocalDateTime.now());
        }
"""

text = text.replace("ticket.setStatus(next);", "ticket.setStatus(next);\n" + script_add)

with open(path, "w") as f:
    f.write(text)

