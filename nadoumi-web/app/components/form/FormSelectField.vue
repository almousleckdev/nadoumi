<script setup lang="ts">
import type { Option } from '~/utils/countries'

/** Label + select + error; the placeholder defaults to the shared "Select…" copy. */
const props = defineProps<{
  id: string
  label: string
  options: Option[]
  error?: string
  required?: boolean
  placeholder?: string
}>()
const model = defineModel<string>({ required: true })
const { t } = useI18n()
const placeholderText = computed(() => props.placeholder ?? t('form.select'))
</script>

<template>
  <NField :label="label" :for="id" :error="error" :required="required">
    <NSelect :id="id" v-model="model" :options="options" :placeholder="placeholderText" :invalid="!!error" />
  </NField>
</template>
