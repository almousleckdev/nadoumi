<script setup lang="ts">
withDefaults(defineProps<{
  id: string
  modelValue: string
  rows?: number
  invalid?: boolean
  placeholder?: string
  maxlength?: number
  disabled?: boolean
}>(), { rows: 4, placeholder: undefined, maxlength: undefined })
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <textarea
    :id="id"
    :rows="rows"
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    :disabled="disabled"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="`${id}-hint ${id}-error`"
    class="w-full rounded-md border px-3 py-2 text-base outline-none focus-visible:border-brand-500"
    :class="invalid ? 'border-red-400' : 'border-slate-200'"
    @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
</template>
