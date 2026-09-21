<script setup lang="ts">
import { PASSPORT_ACCEPT, PASSPORT_MAX_MB, PASSPORT_MIME } from '~/constants/passport'
import type { PassportStatusDto } from '~/types/catalog'
import { readAsDataUrl } from '~/utils/files'
import type { PassportForm } from '~/utils/passportRules'

/**
 * Passport: choose the scan, confirm the details (pre-filled from the profile so the
 * student is not asked to re-type them), then save. The server compares the confirmed
 * details with the profile and refuses a passport that is not valid for more than six months.
 * Collapses to a compact summary once verified and matching — stays expanded whenever
 * something needs the student's attention (nothing uploaded yet, or a mismatch/rejection).
 */
const props = defineProps<{
  applicantId: number
  /** Already entered on the Personal step — prefilled here so the student is not asked again. */
  profile: { givenName: string, familyName: string, dob: string | null }
}>()
const emit = defineEmits<{ changed: [], 'edit-profile': [] }>()
const { t } = useI18n()
const { uploadPassportScan, savePassport, passportStatus, passportScanUrl } = useApplicant()
const { busy, error, notice, run } = useAsyncAction()

const status = ref<PassportStatusDto | null>(null)
const savedScanUrl = ref('')
const file = ref<File | null>(null)
const preview = ref('')
const initial = ref<PassportForm | null>(null)
const expanded = ref(false)

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

async function loadSavedScan() {
  savedScanUrl.value = status.value?.scanUploaded
    ? await passportScanUrl(props.applicantId).then(r => r.url).catch(() => '')
    : ''
}

onMounted(async () => {
  status.value = await passportStatus(props.applicantId).catch(() => null)
  await loadSavedScan()
  initial.value ??= formFromProfileOrSaved()
  expanded.value = cardStatus.value !== 'done'
})

async function onSubmit(form: PassportForm) {
  const saved = await run(async () => {
    if (file.value) await uploadPassportScan(props.applicantId, file.value)
    status.value = await savePassport(props.applicantId, { ...form, readMethod: 'MANUAL', edited: false })
    return true
  })
  if (!saved) return
  file.value = null
  preview.value = ''
  await loadSavedScan()
  // Stay expanded so the student sees the confirmation; the Cancel button below
  // (shown once cardStatus is 'done') lets them collapse it manually.
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
  <DocumentCard :title="t('passport.title')" :guidance="expanded ? t('passport.guidance') : ''" :status="cardStatus">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NAlert v-if="cardStatus === 'done'" tone="success">{{ t('passport.matches') }}</NAlert>

    <div v-if="!expanded" class="flex items-center gap-4">
      <figure v-if="savedScanUrl" class="h-24 w-24 shrink-0 overflow-hidden rounded-lg border border-slate-200 bg-slate-50 sm:h-28 sm:w-28">
        <img :src="savedScanUrl" :alt="t('passport.title')" class="h-full w-full object-cover">
      </figure>
      <div v-else class="grid h-24 w-24 shrink-0 place-items-center rounded-lg border border-slate-200 bg-slate-50 text-slate-400 sm:h-28 sm:w-28">
        <svg viewBox="0 0 24 24" class="h-10 w-10" fill="none" stroke="currentColor" stroke-width="1.4" aria-hidden="true">
          <rect x="4" y="3" width="16" height="18" rx="2" />
          <circle cx="12" cy="9" r="2.5" />
          <path d="M8 16h8" />
        </svg>
      </div>
      <div class="flex-1 text-sm">
        <p class="font-medium text-slate-700">{{ status?.passportNo }}</p>
        <p class="text-slate-500">{{ t('passport.expiresOn', { date: status?.expiryDate }) }}</p>
        <NButton class="mt-2" size="sm" variant="secondary" @click="expanded = true">{{ t('common.edit') }}</NButton>
      </div>
    </div>

    <template v-else>
      <NAlert v-if="cardStatus === 'done'" tone="success">{{ t('passport.matches') }}</NAlert>
      <PassportMismatchNotice v-if="mismatches.length" :mismatches="mismatches" @edit-profile="$emit('edit-profile')" />

      <DocumentPreview v-if="file" :src="preview" :type="file.type" :name="file.name" />
      <DocumentPreview v-else-if="savedScanUrl" :src="savedScanUrl" :name="t('passport.title')" />
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
      <div v-if="cardStatus === 'done'">
        <NButton size="sm" variant="ghost" @click="expanded = false">{{ t('common.cancel') }}</NButton>
      </div>
      <p v-if="notice" class="sr-only" role="status">{{ notice }}</p>
    </template>
  </DocumentCard>
</template>
