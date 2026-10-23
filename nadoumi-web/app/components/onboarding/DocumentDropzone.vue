<script setup lang="ts">
/**
 * Drop zone for documents of many kinds: pick or drop a file, validated before it reaches the parent.
 * Single-image controls (profile photo, passport scan) use the compact `FilePicker` instead.
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
const over = ref(false)

const { error, handleFile, onInput } = useFilePick(() => props, file => emit('select', file))
</script>

<template>
  <div class="grid gap-2">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <label
      :for="inputId"
      class="relative flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed px-4 py-6 text-center transition-colors focus-within:border-brand-500 hover:bg-slate-50"
      :class="over ? 'border-brand-500 bg-brand-50' : 'border-slate-300'"
      @dragover.prevent="over = true"
      @dragleave="over = false"
      @drop.prevent="over = false; handleFile($event.dataTransfer?.files?.[0])"
    >
      <span class="inline-flex items-center rounded-md bg-brand-600 px-4 py-2 text-sm font-semibold text-white">
        {{ replace ? t('onboarding.upload.replace') : t('onboarding.upload.choose') }}
      </span>
      <span v-if="hint" class="text-xs text-slate-500">{{ hint }}</span>
      <input :id="inputId" type="file" :accept="accept" class="sr-only" @change="onInput">
    </label>
  </div>
</template>
