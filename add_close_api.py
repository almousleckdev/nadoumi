import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/web/StudentSupportTicketController.java"
with open(path, "r") as f:
    text = f.read()

script_add = """
    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable Long id) {
        tickets.close(id);
    }
"""

text = text.replace("public MessageResponse reply(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {\n        return tickets.reply(id, req);\n    }", "public MessageResponse reply(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {\n        return tickets.reply(id, req);\n    }\n" + script_add)

with open(path, "w") as f:
    f.write(text)

