import re

file_path = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant/web/StaffApplicantController.java"
with open(file_path, "r") as f:
    text = f.read()

# Delete specific methods by name
methods_to_delete = [
    r'public ApplicantResponse update\(.*?\{.*?^\s*\}',
    r'public void archive\(.*?\{.*?^\s*\}',
    r'public ProtectedMediaResponses\.Uploaded uploadPhoto\(.*?\{.*?^\s*\}',
    r'public EducationResponse addEducation\(.*?\{.*?^\s*\}',
    r'public EducationResponse updateEducation\(.*?\{.*?^\s*\}',
    r'public void deleteEducation\(.*?\{.*?^\s*\}',
    r'public ContactResponse addContact\(.*?\{.*?^\s*\}',
    r'public ContactResponse updateContact\(.*?\{.*?^\s*\}',
    r'public void deleteContact\(.*?\{.*?^\s*\}',
]

for method in methods_to_delete:
    text = re.sub(method, '', text, flags=re.MULTILINE | re.DOTALL)

# Delete their annotations
text = re.sub(r'@PutMapping\("[^"]*"\)\s*@PreAuthorize\("[^"]*"\)\s*@Log\([^)]+\)\s*', '', text)
text = re.sub(r'@DeleteMapping\("[^"]*"\)\s*@ResponseStatus\(HttpStatus\.NO_CONTENT\)\s*@PreAuthorize\("[^"]*"\)\s*@Log\([^)]+\)\s*', '', text)
text = re.sub(r'@PostMapping\("[^"]*"\)\s*@PreAuthorize\("[^"]*"\)\s*@Log\([^)]+\)\s*', '', text)

text = re.sub(r'@PostMapping\("[^"]*"\)\s*@ResponseStatus\(HttpStatus\.CREATED\)\s*@PreAuthorize\("[^"]*"\)\s*', '', text)
text = re.sub(r'@PutMapping\("[^"]*"\)\s*@PreAuthorize\("[^"]*"\)\s*', '', text)
text = re.sub(r'@DeleteMapping\("[^"]*"\)\s*@ResponseStatus\(HttpStatus\.NO_CONTENT\)\s*@PreAuthorize\("[^"]*"\)\s*', '', text)

with open(file_path, "w") as f:
    f.write(text)

