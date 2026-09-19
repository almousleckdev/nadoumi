<script setup lang="ts">
/** Label + textarea + hint + error. */
withDefaults(defineProps<{
  id: string
  label: string
  hint?: string
  error?: string
  rows?: number
  maxlength?: number
}>(), { hint: undefined, error: undefined, rows: 3, maxlength: undefined })
const model = defineModel<string>({ required: true })
</script>

<template>
  <NField :label="label" :for="id" :hint="hint" :error="error">
    <NTextarea :id="id" v-model="model" :rows="rows" :maxlength="maxlength" :invalid="!!error" />
  </NField>
</template>
