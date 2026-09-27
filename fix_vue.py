import re

base = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants"

def patch_file(path, removals):
    with open(path, "r") as f:
        text = f.read()
    for pattern in removals:
        text = re.sub(pattern, '', text, flags=re.DOTALL)
    with open(path, "w") as f:
        f.write(text)

# 1. detail.vue
patch_file(f"{base}/detail.vue", [
    r'<el-tab-pane[^>]*name="scores".*?</el-tab-pane>',
    r"import ScoresTab from '\./tabs/ScoresTab\.vue'",
    r':can-edit="canEdit"',
    r"const canEdit = computed\(\(\) => userStore\.hasPerm\('nad:applicant:edit'\)\)\n",
    r"import \{ useUserStore \} from '@/stores/user'",
])

# 2. OverviewTab.vue
patch_file(f"{base}/tabs/OverviewTab.vue", [
    r'<el-button[^>]*v-if="canEdit".*?</el-button>',
    r'<Drawer[^>]*v-model="open".*?</Drawer>',
    r'const canEdit = inject<Ref<boolean>>\(\'canEdit\'\)\n',
    r"import Drawer from '@/components/ui/Drawer\.vue'\n",
    r"import \{ Edit \} from '@element-plus/icons-vue'\n",
    r"const open = ref\(false\)\n",
    r"const saving = ref\(false\)\n",
    r"const formRef = ref<FormInstance>\(\)\n",
    r"const form = reactive[^)]+\)\n",
    r"const rules = \{[\s\S]*?\}\n\n",
    r"function openEdit\(\) \{[\s\S]*?\}\n\n",
    r"async function save\(\) \{[\s\S]*?\}\n",
])
with open(f"{base}/tabs/OverviewTab.vue", "r") as f:
    t = f.read()
t = t.replace(':disabled="!canEdit"', 'disabled')
t = t.replace("const props = defineProps<{ applicant: Applicant, canEdit: boolean }>()", "const props = defineProps<{ applicant: Applicant }>()")
with open(f"{base}/tabs/OverviewTab.vue", "w") as f:
    f.write(t)

# 3. EducationTab.vue
patch_file(f"{base}/tabs/EducationTab.vue", [
    r'<div class="edu__actions" v-if="canEdit">.*?</el-button>\s*</div>',
    r'<el-button[^>]*@click="edit\(edu\)".*?</el-button>',
    r'<el-popconfirm[^>]*@confirm="remove\(edu\.id\)".*?</el-popconfirm>',
    r'<Drawer[^>]*v-model="open".*?</Drawer>',
    r'const canEdit = inject<Ref<boolean>>\(\'canEdit\'\)\n',
    r"import Drawer from '@/components/ui/Drawer\.vue'\n",
    r"const open = ref\(false\)\n",
    r"const saving = ref\(false\)\n",
    r"const formRef = ref<FormInstance>\(\)\n",
    r"const form = reactive[^)]+\)\n",
    r"const rules = \{[\s\S]*?\}\n\n",
    r"function openCreate\(\) \{[\s\S]*?\}\n\n",
    r"function edit\(row: Education\) \{[\s\S]*?\}\n\n",
    r"async function remove\(id: number\) \{[\s\S]*?\}\n\n",
    r"async function save\(\) \{[\s\S]*?\}\n",
])
with open(f"{base}/tabs/EducationTab.vue", "r") as f:
    t = f.read()
t = t.replace("const props = defineProps<{ id: string, canEdit: boolean }>()", "const props = defineProps<{ id: string }>()")
with open(f"{base}/tabs/EducationTab.vue", "w") as f:
    f.write(t)

# 4. ContactsTab.vue
patch_file(f"{base}/tabs/ContactsTab.vue", [
    r'<div class="con__actions" v-if="canEdit">.*?</el-button>\s*</div>',
    r'<el-button[^>]*@click="edit\(row\)".*?</el-button>',
    r'<el-popconfirm[^>]*@confirm="onDelete\(row\)".*?</el-popconfirm>',
    r'<Drawer[^>]*v-model="open".*?</Drawer>',
    r'const canEdit = inject<Ref<boolean>>\(\'canEdit\'\)\n',
    r"import Drawer from '@/components/ui/Drawer\.vue'\n",
    r"const RELATIONS = \['GUARDIAN', 'EMERGENCY', 'OTHER'\]\n",
    r"const open = ref\(false\)\n",
    r"const saving = ref\(false\)\n",
    r"const formRef = ref<FormInstance>\(\)\n",
    r"const form = reactive[^)]+\)\n",
    r"const rules = \{[\s\S]*?\}\n\n",
    r"function openCreate\(\) \{[\s\S]*?\}\n\n",
    r"function edit\(row: Contact\) \{[\s\S]*?\}\n\n",
    r"async function onDelete\(row: Contact\) \{[\s\S]*?\}\n",
    r"async function save\(\) \{[\s\S]*?\}\n",
])
with open(f"{base}/tabs/ContactsTab.vue", "r") as f:
    t = f.read()
t = t.replace("const props = defineProps<{ id: string, canEdit: boolean }>()", "const props = defineProps<{ id: string }>()")
with open(f"{base}/tabs/ContactsTab.vue", "w") as f:
    f.write(t)

