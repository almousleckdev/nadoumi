<script setup lang="ts">
/** Label + input + hint + error, so no form re-declares the `NField`/`NInput` pairing. */
withDefaults(defineProps<{
  id: string
  label: string
  hint?: string
  error?: string
  required?: boolean
  type?: string
  autocomplete?: string
  maxlength?: number
  min?: string
  max?: string
  disabled?: boolean
  /** Show the value in capitals as it is typed (the server stores it that way). */
  uppercase?: boolean
}>(), {
  hint: undefined, error: undefined, type: 'text', autocomplete: undefined,
  maxlength: undefined, min: undefined, max: undefined,
})
const model = defineModel<string>({ required: true })
</script>

<template>
  <NField :label="label" :for="id" :hint="hint" :error="error" :required="required">
    <NInput
      :id="id"
      v-model="model"
      :type="type"
      :class="{ uppercase }"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      :min="min"
      :max="max"
      :disabled="disabled"
      :invalid="!!error"
    >
      <template v-if="$slots.suffix" #suffix><slot name="suffix" /></template>
    </NInput>
  </NField>
</template>
