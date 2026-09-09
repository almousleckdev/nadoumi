<script setup lang="ts">
import type { GuideIconName } from './icons'

defineProps<{ eyebrow?: string, title: string, icon?: GuideIconName }>()
</script>

<template>
  <section class="border-t border-slate-200 pt-10">
    <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-600">{{ eyebrow }}</p>
    <div class="mt-1 flex items-center gap-3">
      <span
        v-if="icon"
        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700"
      >
        <GuideIcon :name="icon" :size="18" />
      </span>
      <h2 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ title }}</h2>
    </div>
    <div class="mt-4 space-y-4 leading-7 text-slate-700 [&_a]:text-brand-700 [&_a]:underline [&_strong]:font-semibold [&_strong]:text-slate-900">
      <slot />
    </div>
  </section>
</template>
