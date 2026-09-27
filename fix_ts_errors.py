import re

# 1. detail.vue
path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/detail.vue"
with open(path, "r") as f:
    text = f.read()
text = text.replace("@count=\"n => counts.scores = n\"", "")
text = text.replace("const userStore = useUserStore()", "")
text = text.replace("import { changeUserStatus } from '@/api/system'", "import { changeUserStatus } from '@/api/system'\nimport { ElMessage } from 'element-plus'")
with open(path, "w") as f:
    f.write(text)

# 2. EducationTab.vue
path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/tabs/EducationTab.vue"
with open(path, "r") as f:
    text = f.read()
text = text.replace("fetchEducation", "listEducation")
with open(path, "w") as f:
    f.write(text)

# 3. ContactsTab.vue
path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/tabs/ContactsTab.vue"
with open(path, "r") as f:
    text = f.read()
text = text.replace("fetchContacts", "listContacts")
with open(path, "w") as f:
    f.write(text)

