<script setup lang="ts">
import { provide, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import {
  createUniversity, updateUniversity, getUniversity, listDepartments,
  type University,
} from '@/api/university'
import Drawer from '@/components/ui/Drawer.vue'
import { useDeferredImages } from '@/composables/useDeferredImages'
import IdentitySections from './form/IdentitySections.vue'
import ContentSections from './form/ContentSections.vue'
import MediaSections from './form/MediaSections.vue'
import PublishingSection from './form/PublishingSection.vue'
import { syncDepartments } from './syncDepartments'
import {
  blankUniversityForm, buildUniversityPayload, formFromUniversity, universityFormKey,
  type DeptRow,
} from './universityForm'

const props = defineProps<{ modelValue: boolean, university: University | null }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [u: University] }>()

const { t } = useI18n()

const formRef = ref<FormInstance>()
const saving = ref(false)
const media = ref<InstanceType<typeof MediaSections>>()
const deferredImages = useDeferredImages()

// snapshot of the university's departments as loaded, to diff against on save
const originalDepartments = ref<DeptRow[]>([])

const form = reactive(blankUniversityForm())
provide(universityFormKey, form)

const rules = {
  name: [{ required: true, trigger: 'blur', message: t('university.required') }],
  country: [
    { required: true, trigger: 'blur', message: t('university.required') },
    { pattern: /^[A-Za-z]{2}$/, trigger: 'blur', message: t('university.countryFormat') },
  ],
  status: [{ required: true, message: t('university.required') }],
  publishStatus: [{ required: true, message: t('university.required') }],
}

watch(() => props.modelValue, async (open) => {
  if (!open) return
  Object.assign(form, blankUniversityForm())
  originalDepartments.value = []
  if (!props.university) return
  // The list row carries no child collections (rankings / highlights / gallery);
  // fetch the full record so editing shows and preserves them.
  let u: University = props.university
  try {
    u = await getUniversity(props.university.id)
  }
  catch { /* fall back to the row we already have */ }
  Object.assign(form, formFromUniversity(u))
  try {
    const depts = await listDepartments(u.id)
    form.departments = depts.map(d => ({ id: d.id, name: d.name, nameCn: d.nameCn ?? '' }))
    originalDepartments.value = depts.map(d => ({ id: d.id, name: d.name, nameCn: d.nameCn ?? '' }))
  }
  catch { /* no permission or none yet — leave the list empty */ }
}, { immediate: true })

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const saved = props.university
      ? await updateUniversity(props.university.id, buildUniversityPayload(form))
      : await createUniversity(buildUniversityPayload(form))
    const imagesOk = props.university
      ? true
      : await deferredImages.flush(media.value?.uploadTasks(saved.id) ?? [])
    const blocked = await syncDepartments(saved.id, form.departments, originalDepartments.value)
    if (blocked > 0) ElMessage.warning(t('university.departmentsBlocked', { n: blocked }))
    if (imagesOk) ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved', saved)
  }
  finally {
    saving.value = false
  }
}
</script>

<template>
  <Drawer
    :model-value="modelValue"
    :title="university ? t('university.edit') : t('university.new')"
    :saving="saving"
    :size="580"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <IdentitySections />
      <ContentSections />
      <MediaSections
        ref="media"
        :university="university"
      />
      <PublishingSection />
    </el-form>
  </Drawer>
</template>
