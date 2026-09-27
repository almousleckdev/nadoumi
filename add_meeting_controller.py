import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/web/StudentSupportTicketController.java"
with open(path, "r") as f:
    text = f.read()

script_add = """
    @PostMapping("/{id}/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public com.nadoumi.support.domain.SupportMeeting bookMeeting(@PathVariable Long id, @Valid @RequestBody com.nadoumi.support.web.request.BookMeetingRequest req) {
        return tickets.bookMeeting(id, req);
    }
"""

text = text.replace("public void close(@PathVariable Long id) {\n        tickets.close(id);\n    }", "public void close(@PathVariable Long id) {\n        tickets.close(id);\n    }\n" + script_add)

with open(path, "w") as f:
    f.write(text)

