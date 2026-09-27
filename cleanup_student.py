import re

ctrl_file = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant/web/StudentApplicantController.java"
with open(ctrl_file, "r") as f:
    text = f.read()

# remove test score methods
text = re.sub(r'@GetMapping\("/test-scores"\)[^\{]*\{[^\}]*\}', '', text)
text = re.sub(r'@PostMapping\("/test-scores"\)[^\{]*\{[^\}]*\}', '', text)
text = re.sub(r'@PutMapping\("/test-scores/\{scoreId\}"\)[^\{]*\{[^\}]*\}', '', text)
text = re.sub(r'@DeleteMapping\("/test-scores/\{scoreId\}"\)[^\{]*\{[^\}]*\}', '', text)

with open(ctrl_file, "w") as f:
    f.write(text)

