<script setup lang="ts">
export interface FactTile {
  label: string
  value: string
}

defineProps<{ items: FactTile[] }>()
</script>

<template>
  <dl class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-[repeat(auto-fit,minmax(10rem,1fr))]">
    <div
      v-for="f in items"
      :key="f.label"
      class="min-w-0 rounded-xl border border-slate-200 bg-white p-4 shadow-xs"
    >
      <dt class="text-xs font-medium uppercase tracking-wide text-slate-400">{{ f.label }}</dt>
      <dd class="mt-1 break-words font-display text-base font-semibold text-slate-900">{{ f.value }}</dd>
    </div>
  </dl>
</template>
