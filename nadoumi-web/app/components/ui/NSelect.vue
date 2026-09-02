<script setup lang="ts">
defineProps<{
  id: string
  modelValue: string
  options: { value: string; label: string }[]
  invalid?: boolean
  disabled?: boolean
  placeholder?: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <select
    :id="id"
    :value="modelValue"
    :disabled="disabled"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="`${id}-hint ${id}-error`"
    class="w-full rounded-md border px-3 py-2 text-base bg-white outline-none focus-visible:border-brand-500"
    :class="invalid ? 'border-red-400' : 'border-slate-200'"
    @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
  >
    <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
    <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
  </select>
</template>
