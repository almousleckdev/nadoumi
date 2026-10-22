<script setup lang="ts">
export interface PriceRow {
  label: string
  note?: string | null
  /** Shown on the right, e.g. "¥1,000 · $140". */
  price: string
}

defineProps<{ rows: PriceRow[] }>()
</script>

<template>
  <ul class="divide-y divide-slate-100">
    <li
      v-for="(r, i) in rows"
      :key="i"
      class="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between sm:gap-6"
    >
      <span class="min-w-0 text-slate-700">
        {{ r.label }}
        <span v-if="r.note" class="block text-xs text-slate-400">{{ r.note }}</span>
      </span>
      <span class="shrink-0 font-semibold tabular-nums text-slate-900">{{ r.price }}</span>
    </li>
  </ul>
</template>
