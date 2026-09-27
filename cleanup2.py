import re

ctrl_file = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant/web/StaffApplicantController.java"
with open(ctrl_file, "r") as f:
    text = f.read()

# Replace any method annotated with @PostMapping, @PutMapping, @DeleteMapping
pattern = r'@[A-Z][a-z]+Mapping[^\{]*\{[^\}]*\}'

def replacer(match):
    m = match.group(0)
    if "@GetMapping" in m:
        return m
    return ""

new_text = re.sub(r'@(Post|Put|Delete)Mapping[^{]*\{[^\}]*\}', '', text)

with open(ctrl_file, "w") as f:
    f.write(new_text)

