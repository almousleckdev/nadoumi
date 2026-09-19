<script setup lang="ts">
/** Label + input + hint + error, so no form re-declares the `NField`/`NInput` pairing. */
const props = withDefaults(defineProps<{
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
  /** Force the value to UPPERCASE as it is typed (the server stores it that way regardless). */
  uppercase?: boolean
}>(), {
  hint: undefined, error: undefined, type: 'text', autocomplete: undefined,
  maxlength: undefined, min: undefined, max: undefined,
})
const model = defineModel<string>({ required: true })

function onInput(value: string) {
  model.value = props.uppercase ? value.toUpperCase() : value
}
</script>

<template>
  <NField :label="label" :for="id" :hint="hint" :error="error" :required="required">
    <NInput
      :id="id"
      :model-value="model"
      :type="type"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      :min="min"
      :max="max"
      :disabled="disabled"
      :invalid="!!error"
      @update:model-value="onInput"
    >
      <template v-if="$slots.suffix" #suffix><slot name="suffix" /></template>
    </NInput>
  </NField>
</template>
