<script setup lang="ts">
import type { DashboardIconName } from './icons'

/** One headline number with an icon; the brand colour lives on the icon only. */
defineProps<{ icon: DashboardIconName, label: string, value: number | string, hint?: string }>()
</script>

<template>
  <div class="flex min-w-0 items-center gap-3 rounded-xl border border-slate-200 bg-white p-3 sm:gap-4 sm:p-4" data-test="stat-tile">
    <span class="grid h-9 w-9 shrink-0 sm:h-11 sm:w-11 place-items-center rounded-lg bg-slate-100 text-brand-600"><DashboardIcon :name="icon" :size="22" /></span>
    <div class="min-w-0">
      <p class="font-display text-2xl font-bold leading-none tabular-nums text-slate-900" data-test="stat-value">{{ value }}</p>
      <p class="mt-1 text-sm leading-tight text-slate-600">{{ label }}</p>
      <p v-if="hint" class="truncate text-xs text-slate-500">{{ hint }}</p>
    </div>
  </div>
</template>
