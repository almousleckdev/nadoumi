<script setup lang="ts">
import { provide, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { createScholarship, updateScholarship, getScholarship, type Scholarship } from '@/api/scholarship'
import Drawer from '@/components/ui/Drawer.vue'
import { useDeferredImages } from '@/composables/useDeferredImages'
import IdentitySections from './form/IdentitySections.vue'
import EligibilitySections from './form/EligibilitySections.vue'
import FundingSections from './form/FundingSections.vue'
import ApplicationSections from './form/ApplicationSections.vue'
import PublishingSections from './form/PublishingSections.vue'
import { blankForm, buildPayload, formFromScholarship, scholarshipFormKey } from './scholarshipForm'

const props = defineProps<{ modelValue: boolean, scholarship: Scholarship | null }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [s: Scholarship] }>()

const { t } = useI18n()

const formRef = ref<FormInstance>()
const saving = ref(false)
const publishing = ref<InstanceType<typeof PublishingSections>>()
const deferredImages = useDeferredImages()

const form = reactive(blankForm())
provide(scholarshipFormKey, form)

const rules = {
  title: [{ required: true, trigger: 'blur', message: t('scholarship.required') }],
  country: [
    { required: true, trigger: 'blur', message: t('scholarship.required') },
    { pattern: /^[A-Za-z]{2}$/, trigger: 'blur', message: t('scholarship.countryFormat') },
  ],
  fundingModel: [{ required: true, message: t('scholarship.required') }],
  status: [{ required: true, message: t('scholarship.required') }],
  publishStatus: [{ required: true, message: t('scholarship.required') }],
}

watch(() => props.modelValue, async (open) => {
  if (!open) return
  Object.assign(form, blankForm())
  let source = props.scholarship
  if (!source) return
  // the list row carries no child collections (fees / stipends / accommodation /
  // coverage / documents / eligibility); fetch the full aggregate so Edit shows
  // and preserves everything.
  try {
    const full = await getScholarship(source.view.id)
    if (full?.view) source = full
  }
  catch { /* fall back to the row we already have */ }
  Object.assign(form, formFromScholarship(source))
}, { immediate: true })

async function save() {
  await formRef.value?.validate()
  if (!form.levels.length) {
    ElMessage.warning(t('scholarship.needLevel'))
    return
  }
  saving.value = true
  try {
    const saved = props.scholarship
      ? await updateScholarship(props.scholarship.view.id, buildPayload(form))
      : await createScholarship(buildPayload(form))
    const imagesOk = props.scholarship
      ? true
      : await deferredImages.flush(publishing.value?.uploadTasks(saved.view.id) ?? [])
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
    :title="scholarship ? t('scholarship.edit') : t('scholarship.new')"
    :saving="saving"
    :size="640"
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
      <EligibilitySections />
      <FundingSections />
      <ApplicationSections />
      <PublishingSections
        ref="publishing"
        :scholarship="scholarship"
      />
    </el-form>
  </Drawer>
</template>
