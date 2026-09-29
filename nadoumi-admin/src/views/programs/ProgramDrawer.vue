<script setup lang="ts">
import { computed, provide, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { createProgram, updateProgram, isDegree as isDegreeKind, type Program } from '@/api/program'
import { listUniversities, listDepartments, type University, type Department } from '@/api/university'
import Drawer from '@/components/ui/Drawer.vue'
import { useDeferredImages } from '@/composables/useDeferredImages'
import IdentitySections from './form/IdentitySections.vue'
import StructureSections from './form/StructureSections.vue'
import PublishingSections from './form/PublishingSections.vue'
import { blankProgramForm, buildProgramPayload, formFromProgram, programFormKey } from './programForm'

const props = defineProps<{
  modelValue: boolean
  program: Program | null
  lockedUniversity?: { id: number, name: string } | null
}>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [p: Program] }>()

const { t } = useI18n()

const formRef = ref<FormInstance>()
const saving = ref(false)
const publishing = ref<InstanceType<typeof PublishingSections>>()
const deferredImages = useDeferredImages()
const universities = ref<{ id: number, name: string }[]>([])

function loadUniversities() {
  if (props.lockedUniversity || universities.value.length) return
  listUniversities({ page: 0, size: 200 })
    .then(res => universities.value = res.content.map((u: University) => ({ id: u.id, name: u.name })))
    .catch(() => {})
}

// academic departments of the selected university, for the majors picker
const departments = ref<Department[]>([])
async function loadDepartments(universityId: number | null) {
  if (!universityId) { departments.value = []; return }
  try {
    departments.value = await listDepartments(universityId)
  }
  catch { departments.value = [] }
}

const form = reactive(blankProgramForm(props.lockedUniversity?.id ?? null))
provide(programFormKey, form)
const isDegree = computed(() => isDegreeKind(form.programType))

// user picked a different university — its departments no longer apply
async function onUniversityChange() {
  form.majors.forEach(m => { m.departmentId = null })
  await loadDepartments(form.universityId)
}

// dropping a level detaches any major that was assigned to it
watch(() => [...form.levels], (levels) => {
  form.majors.forEach(m => {
    if (m.level && !levels.includes(m.level)) m.level = null
  })
})

const rules = {
  universityId: [{ required: true, message: t('program.required') }],
  name: [{ required: true, trigger: 'blur', message: t('program.required') }],
  programType: [{ required: true, message: t('program.required') }],
  status: [{ required: true, message: t('program.required') }],
  publishStatus: [{ required: true, message: t('program.required') }],
}

watch(() => props.modelValue, (open) => {
  if (!open) return
  loadUniversities()
  Object.assign(form, blankProgramForm(props.lockedUniversity?.id ?? null))
  const p = props.program
  if (!p) {
    loadDepartments(form.universityId)
    return
  }
  Object.assign(form, formFromProgram(p))
  loadDepartments(p.universityId)
}, { immediate: true })

async function save() {
  await formRef.value?.validate()
  if (isDegree.value && !form.levels.length) {
    ElMessage.warning(t('program.levelsRequired'))
    return
  }
  saving.value = true
  try {
    const saved = props.program
      ? await updateProgram(props.program.id, buildProgramPayload(form))
      : await createProgram(buildProgramPayload(form))
    const imagesOk = props.program
      ? true
      : await deferredImages.flush(publishing.value?.uploadTasks(saved.id) ?? [])
    if (imagesOk) ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved', saved)
  }
  finally {
    saving.value = false
  }
}

defineExpose({ form, save, rules })
</script>

<template>
  <Drawer
    :model-value="modelValue"
    :title="program ? t('program.edit') : t('program.new')"
    :saving="saving"
    :size="600"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <IdentitySections
        :universities="universities"
        :locked-university="lockedUniversity"
        @university-change="onUniversityChange"
      />
      <StructureSections :departments="departments" />
      <PublishingSections
        ref="publishing"
        :program="program"
      />
    </el-form>
  </Drawer>
</template>
