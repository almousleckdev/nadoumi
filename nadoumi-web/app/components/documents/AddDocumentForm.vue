<script setup lang="ts">
import { DOCUMENT_ACCEPT, DOCUMENT_MAX_MB, DOCUMENT_MIME } from '~/constants/documents'
import type { DocumentTypeOption } from '~/types/documents'

/** Pick a type from the configurable dictionary, then drop the file. The type must be chosen first. */
defineProps<{ types: DocumentTypeOption[], busy: boolean }>()
const emit = defineEmits<{ submit: [docType: string, file: File] }>()
const { t } = useI18n()
const docType = ref('')
const typeError = ref('')

function onSelect(file: File) {
  if (!docType.value) {
    typeError.value = t('validation.required')
    return
  }
  typeError.value = ''
  emit('submit', docType.value, file)
  docType.value = ''
}
</script>

<template>
  <div class="grid gap-4" :class="busy ? 'pointer-events-none opacity-60' : ''">
    <FormSelectField
      id="docType"
      v-model="docType"
      :label="t('dashboard.docs.typeLabel')"
      :placeholder="t('dashboard.docs.typePlaceholder')"
      :options="types"
      :error="typeError"
      required
    />
    <DocumentDropzone
      :accept="DOCUMENT_ACCEPT"
      :mime="DOCUMENT_MIME"
      :max-mb="DOCUMENT_MAX_MB"
      :bad-type-message="t('dashboard.docs.badType')"
      :hint="t('dashboard.docs.hint')"
      @select="onSelect"
    />
  </div>
</template>
