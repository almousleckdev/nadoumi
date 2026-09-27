import re
path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/detail.vue"
with open(path, "r") as f:
    text = f.read()

# remove duplicate ElMessage
text = re.sub(r"import \{ ElMessage \} from 'element-plus'\n+", "", text)
text = text.replace("import { ChatDotRound }", "import { ElMessage }\nimport { ChatDotRound }")
text = text.replace("const canViewAccess = computed(() => userStore.hasPerm('nad:applicant:access:view'))", "const userStore = useUserStore()\nconst canViewAccess = computed(() => userStore.hasPerm('nad:applicant:access:view'))")

with open(path, "w") as f:
    f.write(text)

