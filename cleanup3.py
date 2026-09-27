import re

ctrl_file = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant/web/StaffApplicantController.java"
with open(ctrl_file, "r") as f:
    text = f.read()

text = re.sub(r'public ApplicantResponse update\([^\{]*\{[^\}]*\}', '', text)
text = re.sub(r'public void archive\([^\{]*\{[^\}]*\}', '', text)

with open(ctrl_file, "w") as f:
    f.write(text)

