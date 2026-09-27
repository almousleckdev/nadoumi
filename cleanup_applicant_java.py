import os
import glob
import re

base_dir = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant"

# 1. Delete TestScore files
test_score_files = [
    f"{base_dir}/domain/ApplicantTestScore.java",
    f"{base_dir}/web/request/TestScoreRequest.java",
    f"{base_dir}/web/response/TestScoreResponse.java",
    f"{base_dir}/mapper/ApplicantTestScoreMapper.java",
]
for f in test_score_files:
    if os.path.exists(f):
        os.remove(f)

# Also check XML
xml_mapper = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/resources/mapper/applicant/ApplicantTestScoreMapper.xml"
if os.path.exists(xml_mapper):
    os.remove(xml_mapper)

# 2. Cleanup ApplicantService.java
service_file = f"{base_dir}/service/ApplicantService.java"
with open(service_file, 'r') as f:
    content = f.read()

# remove test score methods
content = re.sub(r'public List<TestScoreResponse> testScores\(Long applicantId\) \{.*?^\s*\}', '', content, flags=re.MULTILINE | re.DOTALL)
content = re.sub(r'public TestScoreResponse addTestScore\(Long applicantId, TestScoreRequest req\) \{.*?^\s*\}', '', content, flags=re.MULTILINE | re.DOTALL)
content = re.sub(r'public void updateTestScore\(Long applicantId, Long scoreId, TestScoreRequest req\) \{.*?^\s*\}', '', content, flags=re.MULTILINE | re.DOTALL)
content = re.sub(r'public void deleteTestScore\(Long applicantId, Long scoreId\) \{.*?^\s*\}', '', content, flags=re.MULTILINE | re.DOTALL)

with open(service_file, 'w') as f:
    f.write(content)

# 3. Cleanup StaffApplicantController.java
staff_ctrl = f"{base_dir}/web/StaffApplicantController.java"
with open(staff_ctrl, 'r') as f:
    content = f.read()

# remove create, update, archive
content = re.sub(r'@PostMapping\s*@ResponseStatus\(HttpStatus.CREATED\)\s*@PreAuthorize\("@ss.hasPermi\(\'nad:applicant:create\'\)"\)\s*@Log[^\n]+\s*public ApplicantResponse create[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@PutMapping\("/\{id\}"\)\s*@PreAuthorize\("@ss.hasPermi\(\'nad:applicant:edit\'\)"\)\s*@Log[^\n]+\s*public void update[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@DeleteMapping\("/\{id\}"\)\s*@ResponseStatus\(HttpStatus.NO_CONTENT\)\s*@PreAuthorize\("@ss.hasPermi\(\'nad:applicant:delete\'\)"\)\s*@Log[^\n]+\s*public void archive[^{]+\{[^}]+\}', '', content)

# remove photo upload? Rule: MUST NOT Give Admin Editing Capabilities
content = re.sub(r'@PostMapping\("/\{id\}/photo"\)\s*@PreAuthorize\("@ss.hasPermi\(\'nad:applicant:edit\'\)"\)\s*@Log[^\n]+\s*public ProtectedMediaResponses\.Uploaded uploadPhoto[^{]+\{[^}]+\}', '', content)

# remove education mutation
content = re.sub(r'@PostMapping\("/\{id\}/education"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@PutMapping\("/\{id\}/education/\{educationId\}"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@DeleteMapping\("/\{id\}/education/\{educationId\}"\)[^{]+\{[^}]+\}', '', content)

# remove contact mutation
content = re.sub(r'@PostMapping\("/\{id\}/contacts"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@PutMapping\("/\{id\}/contacts/\{contactId\}"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@DeleteMapping\("/\{id\}/contacts/\{contactId\}"\)[^{]+\{[^}]+\}', '', content)

# remove ALL test scores
content = re.sub(r'@GetMapping\("/\{id\}/test-scores"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@PostMapping\("/\{id\}/test-scores"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@PutMapping\("/\{id\}/test-scores/\{scoreId\}"\)[^{]+\{[^}]+\}', '', content)
content = re.sub(r'@DeleteMapping\("/\{id\}/test-scores/\{scoreId\}"\)[^{]+\{[^}]+\}', '', content)

with open(staff_ctrl, 'w') as f:
    f.write(content)

