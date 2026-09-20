<script setup lang="ts">
import { DOCUMENT_ACCEPT, DOCUMENT_LIFECYCLE_TONES, DOCUMENT_MAX_MB, DOCUMENT_MIME, canDeleteDocument } from '~/constants/documents'
import type { StudentDocumentDto } from '~/types/documents'
import { validateFile } from '~/utils/files'
import { formatFileSize } from '~/utils/documents'

/** One of the student's documents: state, version facts, the reviewer's reason if rejected, and its actions. */
const props = defineProps<{ doc: StudentDocumentDto, typeLabel: string, busy: boolean }>()
const emit = defineEmits<{ download: [], replace: [file: File], remove: [] }>()
const { t, locale } = useI18n()
const inputId = useId()
const fileProblem = ref('')

const dateFormat = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }))
const uploadedOn = computed(() =>
  props.doc.currentVersion ? dateFormat.value.format(new Date(props.doc.currentVersion.uploadedAt)) : '')

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  fileProblem.value = validateFile(file, { mime: DOCUMENT_MIME, maxMb: DOCUMENT_MAX_MB })
    ? t('dashboard.docs.badType')
    : ''
  if (!fileProblem.value) emit('replace', file)
}
</script>

<template>
  <li class="grid gap-3 rounded-lg border border-slate-200 bg-white p-4" data-test="document-row">
    <div class="flex flex-wrap items-start justify-between gap-2">
      <div class="min-w-0">
        <h3 class="font-display font-semibold text-slate-900">{{ typeLabel }}</h3>
        <p v-if="doc.currentVersion" class="mt-0.5 text-xs text-slate-500">
          {{ t('dashboard.docs.version', { n: doc.currentVersion.versionNo }) }}
          · {{ formatFileSize(doc.currentVersion.sizeBytes) }}
          · {{ t('dashboard.docs.uploadedOn', { date: uploadedOn }) }}
        </p>
        <p v-if="doc.expiresOn" class="mt-0.5 text-xs text-slate-500">{{ t('dashboard.docs.expiresOn', { date: doc.expiresOn }) }}</p>
      </div>
      <NBadge :tone="DOCUMENT_LIFECYCLE_TONES[doc.status]">{{ t(`dashboard.docs.status.${doc.status}`) }}</NBadge>
    </div>

    <NAlert v-if="doc.status === 'REJECTED' && doc.rejectionReason" tone="danger">
      {{ t('dashboard.docs.reason', { reason: doc.rejectionReason }) }}
    </NAlert>
    <NAlert v-if="fileProblem" tone="danger">{{ fileProblem }}</NAlert>

    <div class="flex flex-wrap items-center gap-2">
      <NButton v-if="doc.currentVersion" size="sm" variant="secondary" :disabled="busy" data-test="doc-download" @click="emit('download')">
        {{ t('dashboard.docs.download') }}
      </NButton>
      <label
        :for="inputId"
        class="inline-flex cursor-pointer items-center rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-900 transition-colors focus-within:border-brand-500 hover:bg-slate-50"
        :class="busy ? 'pointer-events-none opacity-60' : ''"
      >
        {{ t('dashboard.docs.replace') }}
        <input :id="inputId" type="file" :accept="DOCUMENT_ACCEPT" class="sr-only" data-test="doc-replace" :disabled="busy" @change="onPick">
      </label>
      <NButton v-if="canDeleteDocument(doc.status)" size="sm" variant="ghost" :disabled="busy" data-test="doc-delete" @click="emit('remove')">
        {{ t('dashboard.docs.remove') }}
      </NButton>
    </div>
  </li>
</template>
