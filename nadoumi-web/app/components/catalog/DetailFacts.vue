<script setup lang="ts">
import type { DashboardIconName } from '~/components/dashboard/icons'

export interface DetailFact {
  label: string
  value: string
  icon?: DashboardIconName
  /** `price` renders the value red, as every monetary amount on the site. */
  tone?: 'price'
}

defineProps<{ items: DetailFact[] }>()
</script>

<template>
  <dl
    v-if="items.length"
    class="relative z-10 -mt-8 grid grid-cols-1 gap-x-8 gap-y-5 rounded-2xl border border-slate-200 bg-white p-5 shadow-md sm:grid-cols-2 sm:p-6 lg:grid-cols-3"
  >
    <div v-for="f in items" :key="f.label" class="flex min-w-0 items-start gap-3">
      <span v-if="f.icon" class="mt-0.5 shrink-0 text-brand-600">
        <DashboardIcon :name="f.icon" :size="20" />
      </span>
      <div class="min-w-0">
        <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ f.label }}</dt>
        <dd
          class="mt-0.5 break-words font-display text-base font-semibold"
          :class="f.tone === 'price' ? 'text-red-600' : 'text-slate-900'"
        >{{ f.value }}</dd>
      </div>
    </div>
  </dl>
</template>
