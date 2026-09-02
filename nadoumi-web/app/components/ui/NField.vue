<script setup lang="ts">
const props = defineProps<{
  label: string
  for: string
  hint?: string
  error?: string
  required?: boolean
}>()
</script>

<template>
  <div class="grid gap-1.5">
    <label :for="props.for" class="text-sm font-medium text-slate-700">
      {{ label }}<span v-if="required" class="text-brand-700" aria-hidden="true"> *</span>
    </label>
    <slot />
    <p v-if="hint" :id="`${props.for}-hint`" class="text-xs text-slate-500">{{ hint }}</p>
    <p v-if="error" :id="`${props.for}-error`" role="alert" class="text-xs text-red-600">{{ error }}</p>
  </div>
</template>
