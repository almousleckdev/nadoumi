<script setup lang="ts">
import type { GuideIconName } from "./icons"

export interface IconItem {
  icon: GuideIconName
  term: string
  detail: string
}

defineProps<{ items: readonly IconItem[] }>()
</script>

<template>
  <ul class="grid gap-3 sm:grid-cols-2">
    <li
      v-for="it in items"
      :key="it.term"
      class="flex gap-3 rounded-xl border border-slate-200 bg-white p-4"
    >
      <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
        <GuideIcon :name="it.icon" :size="20" />
      </span>
      <div>
        <p class="font-display text-sm font-semibold text-slate-900">{{ it.term }}</p>
        <p class="mt-0.5 text-sm leading-6 text-slate-600">{{ it.detail }}</p>
      </div>
    </li>
  </ul>
</template>
