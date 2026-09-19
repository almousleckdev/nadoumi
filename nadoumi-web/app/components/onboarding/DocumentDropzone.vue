<script setup lang="ts">
import { validateFile } from '~/utils/files'

/**
 * Pick or drop a file, validated against a type and size rule before it reaches the parent.
 * Shared by every upload so the checks and their messages exist once.
 */
const props = defineProps<{
  accept: string
  mime: RegExp
  maxMb: number
  /** Shown when the type is not allowed (it differs per document). */
  badTypeMessage: string
  /** Shown under the label, e.g. the allowed formats. */
  hint?: string
  replace?: boolean
}>()
const emit = defineEmits<{ select: [file: File] }>()
const { t } = useI18n()
const inputId = useId()
const error = ref('')
const over = ref(false)

function handleFile(file: File | undefined) {
  if (!file) return
  const problem = validateFile(file, { mime: props.mime, maxMb: props.maxMb })
  error.value = problem === 'badType' ? props.badTypeMessage : problem === 'tooBig' ? t('onboarding.upload.tooBig', { mb: props.maxMb }) : ''
  if (!problem) emit('select', file)
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  handleFile(input.files?.[0])
  input.value = '' // picking the same file again must still fire
}
</script>

<template>
  <div class="grid gap-2">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <label
      :for="inputId"
      class="flex cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed px-4 py-6 text-center transition-colors focus-within:border-brand-500 hover:bg-slate-50"
      :class="over ? 'border-brand-500 bg-brand-50' : 'border-slate-300'"
      @dragover.prevent="over = true"
      @dragleave="over = false"
      @drop.prevent="over = false; handleFile($event.dataTransfer?.files?.[0])"
    >
      <span class="text-sm font-semibold text-slate-800">
        {{ replace ? t('onboarding.upload.replace') : t('onboarding.upload.choose') }}
      </span>
      <span v-if="hint" class="text-xs text-slate-500">{{ hint }}</span>
      <input :id="inputId" type="file" :accept="accept" class="sr-only" @change="onPick">
    </label>
  </div>
</template>
