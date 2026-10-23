<script setup lang="ts">
import { PASSPORT_ACCEPT, PASSPORT_MAX_MB, PASSPORT_MIME } from '~/constants/passport'
import type { PassportStatusDto } from '~/types/catalog'
import { readAsDataUrl } from '~/utils/files'
import type { PassportForm } from '~/utils/passportRules'

/**
 * Passport: choose the scan, then confirm the number and dates. Names and date of birth are not asked
 * again; the profile is the identity. The server refuses a passport that is not valid for more than
 * six months. A mismatch is only reported for older records that stored holder data of their own.
 */
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [], 'edit-profile': [] }>()
const { t } = useI18n()
const { uploadPassportScan, savePassport, passportStatus, passportScanUrl } = useApplicant()
const { busy, error, notice, run } = useAsyncAction()

const status = ref<PassportStatusDto | null>(null)
const savedScanUrl = ref('')
const file = ref<File | null>(null)
const preview = ref('')
const initial = ref<PassportForm | null>(null)

function formFrom(saved: PassportStatusDto | null): PassportForm | null {
  if (!saved?.passportNo) return null
  return { passportNo: saved.passportNo, issueDate: saved.issueDate ?? '', expiryDate: saved.expiryDate ?? '' }
}

async function loadSavedScan() {
  savedScanUrl.value = status.value?.scanUploaded
    ? await passportScanUrl(props.applicantId).then(r => r.url).catch(() => '')
    : ''
}

onMounted(async () => {
  status.value = await passportStatus(props.applicantId).catch(() => null)
  await loadSavedScan()
  initial.value = formFrom(status.value)
})

async function onSelect(picked: File) {
  file.value = picked
  preview.value = picked.type === 'application/pdf' ? '' : await readAsDataUrl(picked)
}

async function onSubmit(form: PassportForm) {
  const saved = await run(async () => {
    if (file.value) await uploadPassportScan(props.applicantId, file.value)
    status.value = await savePassport(props.applicantId, { ...form, readMethod: 'MANUAL', edited: false })
    return true
  }, t('passport.saved'))
  if (!saved) return
  file.value = null
  preview.value = ''
  initial.value = formFrom(status.value)
  await loadSavedScan()
  emit('changed')
}

const hasScan = computed(() => Boolean(file.value) || status.value?.scanUploaded === true)
const showForm = computed(() => hasScan.value || Boolean(status.value?.passportNo))
const mismatches = computed(() => (status.value?.passportNo && !status.value.matchesProfile ? status.value.mismatches : []))
const cardStatus = computed(() => {
  if (!status.value?.passportNo) return 'pending'
  return status.value.scanUploaded && status.value.validForAdmission && status.value.matchesProfile ? 'done' : 'attention'
})
</script>

<template>
  <DocumentCard :title="t('passport.title')" :guidance="t('passport.guidance')" :status="cardStatus">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NAlert v-if="notice" tone="success">{{ notice }}</NAlert>
    <PassportMismatchNotice v-if="mismatches.length" :mismatches="mismatches" @edit-profile="$emit('edit-profile')" />

    <FilePicker
      shape="document"
      :accept="PASSPORT_ACCEPT"
      :mime="PASSPORT_MIME"
      :max-mb="PASSPORT_MAX_MB"
      :bad-type-message="t('onboarding.upload.badTypePassport')"
      :src="preview || savedScanUrl"
      :pdf="file?.type === 'application/pdf'"
      :label="hasScan ? t('onboarding.upload.replace') : t('onboarding.upload.choose')"
      :hint="file ? file.name : hasScan ? t('passport.formats') : t('passport.none')"
      @select="onSelect"
    />

    <PassportDetailsForm v-if="showForm" :initial="initial" :busy="busy" :can-save="hasScan" @submit="onSubmit" />
  </DocumentCard>
</template>
