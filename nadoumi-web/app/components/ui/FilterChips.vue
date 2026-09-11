<script setup lang="ts">
import type { ActiveChip } from '~/composables/useDiscovery'

defineProps<{ chips: ActiveChip[], clearLabel?: string }>()
const emit = defineEmits<{ remove: [chip: ActiveChip], clear: [] }>()
</script>

<template>
  <div v-if="chips.length" class="flex flex-wrap items-center gap-2">
    <button
      v-for="chip in chips"
      :key="`${chip.key}:${chip.value}`"
      type="button"
      class="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-white py-1 ps-3 pe-2 text-sm text-slate-700 hover:border-slate-300"
      @click="emit('remove', chip)"
    >
      {{ chip.label }}
      <span class="text-slate-400" aria-hidden="true">×</span>
      <span class="sr-only">Remove filter</span>
    </button>
    <button
      type="button"
      class="text-sm font-medium text-brand-700 hover:text-brand-800"
      @click="emit('clear')"
    >
      {{ clearLabel || 'Clear all' }}
    </button>
  </div>
</template>
