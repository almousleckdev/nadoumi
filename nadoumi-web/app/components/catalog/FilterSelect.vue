<script setup lang="ts">
/** A single-choice catalog filter. The option with value '' is the "any" choice. */
defineProps<{
  value?: string | string[] | null
  /** Accessible name; the control has no visible label. */
  label: string
  options: { value: string, label: string }[]
}>()
const emit = defineEmits<{ commit: [value: string] }>()
</script>

<template>
  <select
    :value="typeof value === 'string' ? value : ''"
    :aria-label="label"
    class="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
    @change="emit('commit', ($event.target as HTMLSelectElement).value)"
  >
    <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
  </select>
</template>
