import re
base = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants"

# 1. OverviewTab
ov = f"{base}/tabs/OverviewTab.vue"
with open(ov, "r") as f:
    text = f.read()
text = re.sub(r'const props = defineProps<\{ applicant: Applicant, canEdit: boolean \}>\(\)', 'const props = defineProps<{ applicant: Applicant }>()', text)
text = re.sub(r"import Drawer from '@/components/ui/Drawer.vue'", "", text)
text = re.sub(r"import \{ Edit \} from '@element-plus/icons-vue'", "", text)
text = re.sub(r"const rules = \{[\s\S]*?\n\}", "", text)
text = re.sub(r"function openEdit\(\) \{[\s\S]*?\}", "", text)
text = re.sub(r"async function save\(\) \{[\s\S]*?\}", "", text)
with open(ov, "w") as f:
    f.write(text)

# 2. EducationTab
ed = f"{base}/tabs/EducationTab.vue"
with open(ed, "r") as f:
    text = f.read()
text = re.sub(r'const props = defineProps<\{ id: string, canEdit: boolean \}>\(\)', 'const props = defineProps<{ id: string }>()', text)
text = re.sub(r"import Drawer from '@/components/ui/Drawer.vue'", "", text)
text = re.sub(r"const rules = \{[\s\S]*?\n\}", "", text)
text = re.sub(r"async function save\(\) \{[\s\S]*?\}", "", text)
with open(ed, "w") as f:
    f.write(text)

# 3. ContactsTab
co = f"{base}/tabs/ContactsTab.vue"
with open(co, "r") as f:
    text = f.read()
text = re.sub(r'const props = defineProps<\{ id: string, canEdit: boolean \}>\(\)', 'const props = defineProps<{ id: string }>()', text)
text = re.sub(r"import Drawer from '@/components/ui/Drawer.vue'", "", text)
text = re.sub(r"const RELATIONS = \['GUARDIAN', 'EMERGENCY', 'OTHER'\]", "", text)
text = re.sub(r"const rules = \{[\s\S]*?\n\}", "", text)
text = re.sub(r"async function save\(\) \{[\s\S]*?\}", "", text)
with open(co, "w") as f:
    f.write(text)

