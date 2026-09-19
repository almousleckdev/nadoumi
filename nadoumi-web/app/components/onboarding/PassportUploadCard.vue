<script setup lang="ts">
import {
  PASSPORT_ACCEPT, PASSPORT_MAX_MB, PASSPORT_MIME, PASSPORT_READ_FIELDS, type PassportReadState,
} from '~/constants/passport'
import type { Ref } from 'vue'
import type { PassportStatusDto } from '~/types/catalog'
import type { PassportReading } from '~/services/passport/types'
import { readAsDataUrl } from '~/utils/files'
import type { PassportForm } from '~/utils/passportRules'

/**
 * Passport: choose the scan, read it in the browser, let the student confirm the details,
 * then save. The server compares the confirmed details with the profile and refuses a
 * passport that is not valid for more than six months.
 */
const props = defineProps<{ applicantId: number }>()
const emit = defineEmits<{ changed: [], 'edit-profile': [] }>()
const { t } = useI18n()
const { uploadPassportScan, savePassport, passportStatus } = useApplicant()
const reader = usePassportReader()
const { busy, error, notice, run } = useAsyncAction()

const status = ref<PassportStatusDto | null>(null)
const file = ref<File | null>(null)
const preview = ref('')
const readState: Ref<PassportReadState> = ref('idle')
const reading = ref<PassportForm | null>(null)
const initial = ref<PassportForm | null>(null)

function formFrom(saved: PassportStatusDto | null): PassportForm | null {
  if (!saved?.passportNo) return null
  return {
    passportNo: saved.passportNo, givenName: saved.givenName ?? '', familyName: saved.familyName ?? '',
    dob: saved.dob ?? '', issueDate: saved.issueDate ?? '', expiryDate: saved.expiryDate ?? '',
  }
}

/** Expiry and issue dates are not in the reading, so those start empty for the student to fill. */
function formFromReading(r: PassportReading): PassportForm {
  return {
    passportNo: r.documentNumber, givenName: r.givenNames, familyName: r.surname,
    dob: r.dateOfBirth, issueDate: '', expiryDate: r.expiryDate,
  }
}

async function onSelect(picked: File) {
  file.value = picked
  preview.value = await readAsDataUrl(picked)
  reading.value = null
  if (picked.type === 'application/pdf') {
    readState.value = 'manual'
    return
  }
  readState.value = 'reading'
  const result = await reader.read(picked).catch(() => null)
  if (!result) {
    readState.value = 'unreadable'
    return
  }
  reading.value = formFromReading(result)
  initial.value = reading.value
  readState.value = 'read'
}

/** True when the student changed anything the reader produced. */
const wasEdited = (form: PassportForm) =>
  reading.value !== null && PASSPORT_READ_FIELDS.some(field => form[field] !== reading.value![field])

onMounted(async () => {
  status.value = await passportStatus(props.applicantId).catch(() => null)
  initial.value ??= formFrom(status.value)
})

async function onSubmit(form: PassportForm) {
  const saved = await run(async () => {
    if (file.value) await uploadPassportScan(props.applicantId, file.value)
    status.value = await savePassport(props.applicantId, {
      ...form,
      readMethod: reading.value ? 'MRZ' : 'MANUAL',
      edited: wasEdited(form),
    })
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
const READ_MESSAGE_KEYS: Record<PassportReadState, string | null> = {
  idle: null, reading: 'passport.reading', read: 'passport.readOk',
  unreadable: 'passport.readFailed', manual: 'passport.pdfManual',
}
const readMessage = computed(() => {
  const key = READ_MESSAGE_KEYS[readState.value]
  return key ? t(key) : ''
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

    <p v-if="readMessage" class="flex items-center gap-2 text-sm" :class="readState === 'unreadable' ? 'text-amber-800' : 'text-slate-600'" role="status">
      <NSpinner v-if="readState === 'reading'" />{{ readMessage }}
    </p>

    <PassportDetailsForm v-if="showForm" :initial="initial" :busy="busy" :can-save="hasScan && readState !== 'reading'" @submit="onSubmit" />
    <p v-if="notice" class="sr-only" role="status">{{ notice }}</p>
  </DocumentCard>
</template>
