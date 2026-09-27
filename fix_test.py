import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/test/java/com/nadoumi/support/service/SupportTicketServiceTest.java"
with open(path, "r") as f:
    text = f.read()

text = text.replace("private final NadoumiAccessService access = mock(NadoumiAccessService.class);", "private final NadoumiAccessService access = mock(NadoumiAccessService.class);\n    private final com.nadoumi.support.mapper.SupportMeetingMapper meetingMapper = mock(com.nadoumi.support.mapper.SupportMeetingMapper.class);")

with open(path, "w") as f:
    f.write(text)

