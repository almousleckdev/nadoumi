import re

paths = [
    "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/web/response/StaffTicketSummary.java",
    "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/web/response/StudentTicketSummary.java"
]

for path in paths:
    with open(path, "r") as f:
        text = f.read()
    
    text = text.replace("LocalDateTime updateTime)", "LocalDateTime updateTime,\n        LocalDateTime resolvedAt,\n        LocalDateTime closedAt)")
    text = text.replace("t.getUpdateTime());", "t.getUpdateTime(),\n                t.getResolvedAt(),\n                t.getClosedAt());")
    
    with open(path, "w") as f:
        f.write(text)

