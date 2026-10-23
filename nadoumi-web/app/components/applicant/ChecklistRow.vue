<script setup lang="ts">
import { DOCUMENT_ACCEPT, DOCUMENT_LIFECYCLE_TONES, DOCUMENT_MAX_MB, DOCUMENT_MIME } from '~/constants/documents'
import type { ChecklistItemDto } from '~/types/checklist'
import type { ApplicationTemplateDto } from '~/types/template'
import { previewKind } from '~/utils/documents'
import { validateFile } from '~/utils/files'

/** One document the application needs: what it is, where the student stands, and the way to upload it. */
const props = defineProps<{
  item: ChecklistItemDto
  busy: boolean
  /** The type of the stored file, when known, so a picture or PDF can be viewed. */
  contentType?: string | null
  /** The forms to download for the application form row. */
  forms?: ApplicationTemplateDto[]
  downloadingForm?: number | null
}>()
const emit = defineEmits<{ upload: [file: File], view: [], 'download-form': [form: ApplicationTemplateDto] }>()
const { t, te } = useI18n()
const inputId = useId()
const problem = ref('')

const label = computed(() => te(`scholarships.doc.${props.item.docType}`) ? t(`scholarships.doc.${props.item.docType}`) : props.item.docType)
const tone = computed(() => props.item.status === 'MISSING' ? 'neutral' : DOCUMENT_LIFECYCLE_TONES[props.item.status])
const canView = computed(() => props.item.documentId !== null && !!props.contentType && previewKind(props.contentType) !== null)
const isForm = computed(() => props.item.docType === 'APPLICATION_FORM')
const needsUpload = computed(() => props.item.status === 'MISSING' || props.item.status === 'REJECTED' || props.item.status === 'EXPIRED')

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  problem.value = validateFile(file, { mime: DOCUMENT_MIME, maxMb: DOCUMENT_MAX_MB }) ? t('checklist.badType') : ''
  if (!problem.value) emit('upload', file)
}
</script>

<template>
  <li class="grid gap-2 py-3 first:pt-0 last:pb-0" data-test="checklist-row">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div class="min-w-0">
        <p class="font-medium text-slate-900">
          {{ label }}
          <span class="ms-1 text-xs font-normal text-slate-500">{{ item.mandatory ? t('checklist.mandatory') : t('checklist.optional') }}</span>
        </p>
        <p v-if="item.note && !isForm" class="text-sm text-slate-500">{{ item.note }}</p>
        <p v-if="isForm" class="text-sm text-slate-500">{{ t('checklist.formSteps') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <NButton v-if="canView" variant="secondary" size="sm" :disabled="busy" data-test="checklist-view" @click="emit('view')">
          {{ t('checklist.view') }}
        </NButton>
        <NBadge :tone="tone">{{ item.status === 'MISSING' ? t('checklist.status.MISSING') : t(`dashboard.docs.status.${item.status}`) }}</NBadge>
        <label
          v-if="needsUpload"
          :for="inputId"
          class="relative inline-flex cursor-pointer items-center rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-900 transition-colors focus-within:border-brand-500 hover:bg-slate-50"
          :class="busy ? 'pointer-events-none opacity-60' : ''"
        >
          {{ item.status === 'MISSING' ? t('checklist.upload') : t('checklist.uploadAgain') }}
          <input :id="inputId" type="file" :accept="DOCUMENT_ACCEPT" class="sr-only" :data-test="`upload-${item.docType}`" :disabled="busy" @change="onPick">
        </label>
      </div>
    </div>
    <ul v-if="isForm && forms?.length" class="grid gap-2" data-test="form-downloads">
      <li v-for="form in forms" :key="form.id" class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 px-3 py-2">
        <span class="min-w-0">
          <span class="block text-sm font-medium text-slate-900">{{ form.title }}</span>
          <span class="block truncate text-xs text-slate-500">{{ form.fileName }}</span>
        </span>
        <NButton variant="secondary" size="sm" :loading="downloadingForm === form.id" :data-test="`form-${form.id}`" @click="emit('download-form', form)">
          {{ t('templates.download') }}
        </NButton>
      </li>
    </ul>
    <NAlert v-if="item.status === 'REJECTED' && item.rejectionReason" tone="danger">
      {{ t('dashboard.docs.reason', { reason: item.rejectionReason }) }}
    </NAlert>
    <NAlert v-if="problem" tone="danger">{{ problem }}</NAlert>
  </li>
</template>
