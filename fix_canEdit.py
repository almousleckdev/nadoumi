import re

def fix(path):
    with open(path, "r") as f:
        t = f.read()
    
    # remove buttons guarded by canEdit
    t = re.sub(r'<el-button[^>]*v-if="canEdit".*?</el-button>', '', t, flags=re.DOTALL)
    t = re.sub(r'<div class="edu__actions" v-if="canEdit">.*?</div>', '', t, flags=re.DOTALL)
    t = re.sub(r'<div class="con__actions" v-if="canEdit">.*?</div>', '', t, flags=re.DOTALL)
    
    # remove props definition
    t = t.replace('canEdit: boolean', '')
    
    with open(path, "w") as f:
        f.write(t)

fix("/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/tabs/EducationTab.vue")
fix("/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/tabs/ContactsTab.vue")
