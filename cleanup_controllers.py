import re

def clean_controller(file_path):
    with open(file_path, "r") as f:
        text = f.read()
    
    # Remove all TestScore imports
    text = re.sub(r'import com\.nadoumi\.applicant\.web\.(request|response)\.TestScore(Request|Response);\n', '', text)
    
    # Remove test-scores methods
    # We can match from @GetMapping("/{id}/test-scores") up to the next // ---- contacts ---- comment or @GetMapping
    text = re.sub(r'@GetMapping\("/(\{id\}/)?test-scores"\).*?(?=@GetMapping\("/(\{id\}/)?contacts"\)|\Z)', '', text, flags=re.DOTALL)
    
    with open(file_path, "w") as f:
        f.write(text)

clean_controller("/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant/web/StaffApplicantController.java")
clean_controller("/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-applicant/src/main/java/com/nadoumi/applicant/web/StudentApplicantController.java")

