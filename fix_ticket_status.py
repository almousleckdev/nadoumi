import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/domain/enums/TicketStatus.java"
with open(path, "r") as f:
    text = f.read()

text = text.replace("case OPEN -> Set.of(IN_PROGRESS);", "case OPEN -> Set.of(IN_PROGRESS, CLOSED);")
text = text.replace("case IN_PROGRESS -> Set.of(WAITING_ON_STUDENT, RESOLVED);", "case IN_PROGRESS -> Set.of(WAITING_ON_STUDENT, RESOLVED, CLOSED);")
text = text.replace("case WAITING_ON_STUDENT -> Set.of(IN_PROGRESS, RESOLVED);", "case WAITING_ON_STUDENT -> Set.of(IN_PROGRESS, RESOLVED, CLOSED);")

with open(path, "w") as f:
    f.write(text)

