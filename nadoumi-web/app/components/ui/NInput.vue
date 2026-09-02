<script setup lang="ts">
const props = withDefaults(defineProps<{
  id: string
  modelValue: string
  type?: string
  invalid?: boolean
  autocomplete?: string
  placeholder?: string
  maxlength?: number
  disabled?: boolean
}>(), { type: 'text', autocomplete: undefined, placeholder: undefined, maxlength: undefined })
defineEmits<{ 'update:modelValue': [value: string] }>()

const describedBy = `${props.id}-hint ${props.id}-error`
</script>

<template>
  <input
    :id="id"
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    :disabled="disabled"
    :autocomplete="autocomplete"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="describedBy"
    class="w-full rounded-md border px-3 py-2 text-base outline-none focus-visible:border-brand-500"
    :class="invalid ? 'border-red-400' : 'border-slate-200'"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  >
</template>
