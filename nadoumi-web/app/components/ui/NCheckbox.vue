<script setup lang="ts">
defineProps<{ id: string; modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<template>
  <label :for="id" class="flex items-start gap-2 text-sm text-slate-700">
    <input
      :id="id"
      type="checkbox"
      :checked="modelValue"
      class="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus-visible:outline-brand-500"
      @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
    >
    <span><slot /></span>
  </label>
</template>
