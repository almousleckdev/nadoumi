<script setup lang="ts">
/** A yes/no catalog filter shown as a labelled checkbox. */
defineProps<{ checked: boolean, label: string }>()
const emit = defineEmits<{ commit: [checked: boolean] }>()
</script>

<template>
  <label class="inline-flex items-center gap-1.5 text-sm text-slate-700">
    <input
      type="checkbox"
      :checked="checked"
      class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
      @change="emit('commit', ($event.target as HTMLInputElement).checked)"
    >
    {{ label }}
  </label>
</template>
