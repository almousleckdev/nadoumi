<script setup lang="ts">
import type { PreviewFile } from '~/utils/documents'

/** A read-only look at an uploaded document: images inline, PDFs in the browser's viewer. */
defineProps<{ modelValue: boolean, file: PreviewFile | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const { t } = useI18n()
</script>

<template>
  <NModal :model-value="modelValue" :title="file?.title" wide @update:model-value="emit('update:modelValue', $event)">
    <img
      v-if="file?.kind === 'image'"
      :src="file.src"
      :alt="file.title"
      class="mx-auto max-h-[70vh] w-auto max-w-full rounded-md border border-slate-200"
      data-test="preview-image"
    >
    <iframe
      v-else-if="file"
      :src="file.src"
      :title="file.title"
      class="h-[70vh] w-full rounded-md border border-slate-200"
      data-test="preview-pdf"
    />
    <template #footer>
      <NButton variant="secondary" @click="emit('update:modelValue', false)">{{ t('common.close') }}</NButton>
    </template>
  </NModal>
</template>
