<script setup lang="ts">
import { PASSPORT_ACCEPT, PASSPORT_MAX_MB, PASSPORT_MIME } from '~/constants/passport'
import type { PassportStatusDto } from '~/types/catalog'
import { readAsDataUrl } from '~/utils/files'
import type { PassportForm } from '~/utils/passportRules'

/**
 * Passport: choose the scan, confirm the details (pre-filled from the profile so the
 * student is not asked to re-type them), then save. The server compares the confirmed
 * details with the profile and refuses a passport that is not valid for more than six months.
 */
const props = defineProps<{
  applicantId: number
  /** Already entered on the Personal step — prefilled here so the student is not asked again. */
  profile: { givenName: string, familyName: string, dob: string | null }
}>()
const emit = defineEmits<{ changed: [], 'edit-profile': [] }>()
const { t } = useI18n()
const { uploadPassportScan, savePassport, passportStatus } = useApplicant()
const { busy, error, notice, run } = useAsyncAction()

const status = ref<PassportStatusDto | null>(null)
const file = ref<File | null>(null)
const preview = ref('')
const initial = ref<PassportForm | null>(null)

function formFrom(saved: PassportStatusDto | null): PassportForm | null {
  if (!saved?.passportNo) return null
  return {
    passportNo: saved.passportNo, givenName: saved.givenName ?? '', familyName: saved.familyName ?? '',
    dob: saved.dob ?? '', issueDate: saved.issueDate ?? '', expiryDate: saved.expiryDate ?? '',
  }
}

/** Nothing saved yet: start from what the student already gave us on Personal. */
function formFromProfile(): PassportForm {
  return {
    passportNo: '', givenName: props.profile.givenName, familyName: props.profile.familyName,
    dob: props.profile.dob ?? '', issueDate: '', expiryDate: '',
  }
}

async function onSelect(picked: File) {
  file.value = picked
  preview.value = await readAsDataUrl(picked)
}

function formFromProfileOrSaved(): PassportForm {
  return formFrom(status.value) ?? formFromProfile()
}

onMounted(async () => {
  status.value = await passportStatus(props.applicantId).catch(() => null)
  initial.value ??= formFromProfileOrSaved()
})

async function onSubmit(form: PassportForm) {
  const saved = await run(async () => {
    if (file.value) await uploadPassportScan(props.applicantId, file.value)
    status.value = await savePassport(props.applicantId, { ...form, readMethod: 'MANUAL', edited: false })
    return true
  })
  if (!saved) return
  file.value = null
  emit('changed')
}

const hasScan = computed(() => Boolean(file.value) || status.value?.scanUploaded === true)
const showForm = computed(() => Boolean(file.value) || Boolean(status.value?.passportNo))
const mismatches = computed(() => (status.value?.passportNo && !status.value.matchesProfile ? status.value.mismatches : []))
const cardStatus = computed(() => {
  if (!status.value?.passportNo) return 'pending'
  return status.value.scanUploaded && status.value.validForAdmission && status.value.matchesProfile ? 'done' : 'attention'
})
</script>

<template>
  <DocumentCard :title="t('passport.title')" :guidance="t('passport.guidance')" :status="cardStatus">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NAlert v-if="cardStatus === 'done'" tone="success">{{ t('passport.matches') }}</NAlert>
    <PassportMismatchNotice v-if="mismatches.length" :mismatches="mismatches" @edit-profile="$emit('edit-profile')" />

    <DocumentPreview v-if="file" :src="preview" :type="file.type" :name="file.name" />
    <p v-else-if="!status?.scanUploaded" class="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-400">
      {{ t('passport.none') }}
    </p>

    <DocumentDropzone
      :accept="PASSPORT_ACCEPT"
      :mime="PASSPORT_MIME"
      :max-mb="PASSPORT_MAX_MB"
      :bad-type-message="t('onboarding.upload.badTypePassport')"
      :hint="t('passport.formats')"
      :replace="hasScan"
      @select="onSelect"
    />

    <PassportDetailsForm v-if="showForm" :initial="initial" :busy="busy" :can-save="hasScan" @submit="onSubmit" />
    <p v-if="notice" class="sr-only" role="status">{{ notice }}</p>
  </DocumentCard>
</template>
