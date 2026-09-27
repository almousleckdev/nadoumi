import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/detail.vue"
with open(path, "r") as f:
    text = f.read()

# remove ScoresTab import and `<el-tab-pane>` again (if it wasn't removed)
text = re.sub(r'<el-tab-pane[^>]*name="scores".*?</el-tab-pane>', '', text, flags=re.DOTALL)
text = re.sub(r"import ScoresTab from '\./tabs/ScoresTab\.vue'\n", "", text)
text = re.sub(r"import \{ useUserStore \} from '@/stores/user'\n", "", text)
text = re.sub(r":can-edit=\"canEdit\"", "", text)
text = re.sub(r"const canEdit = computed[^\n]*\n", "", text)

# add actions
actions_replacement = """<template #actions>
          <el-button size="small" @click="router.push(`/conversations?userId=${applicant.id}`)">
            <el-icon><ChatDotRound /></el-icon> Contact
          </el-button>
          <el-button size="small" type="warning" plain @click="changeStatus('2')">
            Suspend
          </el-button>
          <el-button size="small" type="danger" plain @click="changeStatus('1')">
            Block
          </el-button>
          <StatusBadge :status="applicant.status" />
        </template>"""
text = re.sub(r'<template #actions>\s*<StatusBadge :status="applicant\.status" />\s*</template>', actions_replacement, text)

# imports
imports_to_add = "import { ChatDotRound } from '@element-plus/icons-vue'\nimport { changeUserStatus } from '@/api/system'\n"
text = text.replace("import { useI18n } from 'vue-i18n'", imports_to_add + "import { useI18n } from 'vue-i18n'")

# script to add changeStatus
status_script = """
async function changeStatus(status: string) {
  if (!applicant.value) return
  await changeUserStatus(applicant.value.id, status)
  ElMessage.success('Status updated successfully')
  load()
}
"""
text = text.replace("const router = useRouter()", "const router = useRouter()" + status_script)

with open(path, "w") as f:
    f.write(text)

