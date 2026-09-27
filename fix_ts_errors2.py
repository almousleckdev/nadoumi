import re

# detail.vue
path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/detail.vue"
with open(path, "r") as f:
    text = f.read()
text = text.replace("const canViewAccess = computed(() => userStore.hasPerm('nad:applicant:access:view'))", "const userStore = useUserStore()\nconst canViewAccess = computed(() => userStore.hasPerm('nad:applicant:access:view'))")
text = text.replace("import { changeUserStatus } from '@/api/system'", "import { changeUserStatus } from '@/api/system'\nimport { useUserStore } from '@/stores/user'")
with open(path, "w") as f:
    f.write(text)

