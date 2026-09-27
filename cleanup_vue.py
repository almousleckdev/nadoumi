import re
import os

base = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants"

# 1. detail.vue
detail = f"{base}/detail.vue"
with open(detail, "r") as f:
    text = f.read()
text = re.sub(r'<el-tab-pane[^>]*name="scores".*?</el-tab-pane>', '', text, flags=re.DOTALL)
text = text.replace("import ScoresTab from './tabs/ScoresTab.vue'", "")
text = re.sub(r':can-edit="canEdit"', '', text)
text = re.sub(r'const canEdit = computed\(\(\) => userStore\.hasPerm\(\'nad:applicant:edit\'\)\)', '', text)
with open(detail, "w") as f:
    f.write(text)

# 2. OverviewTab.vue
overview = f"{base}/tabs/OverviewTab.vue"
with open(overview, "r") as f:
    text = f.read()
# remove the edit button and the drawer entirely
text = re.sub(r'<el-button[^>]*v-if="canEdit".*?</el-button>', '', text, flags=re.DOTALL)
text = re.sub(r'<Drawer[^>]*v-model="open".*?</Drawer>', '', text, flags=re.DOTALL)
text = re.sub(r':disabled="!canEdit"', 'disabled', text)
text = re.sub(r'const canEdit = inject<Ref<boolean>>\(\'canEdit\'\)', '', text)
with open(overview, "w") as f:
    f.write(text)

# 3. EducationTab.vue
edu = f"{base}/tabs/EducationTab.vue"
with open(edu, "r") as f:
    text = f.read()
text = re.sub(r'<div class="edu__actions" v-if="canEdit">.*?</el-button>\s*</div>', '', text, flags=re.DOTALL)
text = re.sub(r'<el-button[^>]*@click="edit\(edu\)".*?</el-button>', '', text, flags=re.DOTALL)
text = re.sub(r'<el-popconfirm[^>]*@confirm="remove\(edu\.id\)".*?</el-popconfirm>', '', text, flags=re.DOTALL)
text = re.sub(r'<Drawer[^>]*v-model="open".*?</Drawer>', '', text, flags=re.DOTALL)
text = re.sub(r'const canEdit = inject<Ref<boolean>>\(\'canEdit\'\)', '', text)
with open(edu, "w") as f:
    f.write(text)

# 4. ContactsTab.vue
contacts = f"{base}/tabs/ContactsTab.vue"
with open(contacts, "r") as f:
    text = f.read()
text = re.sub(r'<div class="con__actions" v-if="canEdit">.*?</el-button>\s*</div>', '', text, flags=re.DOTALL)
text = re.sub(r'<el-button[^>]*@click="edit\(row\)".*?</el-button>', '', text, flags=re.DOTALL)
text = re.sub(r'<el-popconfirm[^>]*@confirm="remove\(row\.id\)".*?</el-popconfirm>', '', text, flags=re.DOTALL)
text = re.sub(r'<Drawer[^>]*v-model="open".*?</Drawer>', '', text, flags=re.DOTALL)
text = re.sub(r'const canEdit = inject<Ref<boolean>>\(\'canEdit\'\)', '', text)
with open(contacts, "w") as f:
    f.write(text)

