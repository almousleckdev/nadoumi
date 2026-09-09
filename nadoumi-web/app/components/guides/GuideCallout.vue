<script setup lang="ts">
import type { GuideIconName } from "./icons"

defineProps<{ title?: string, icon?: GuideIconName, tone?: 'brand' | 'amber' | 'slate' }>()
</script>

<template>
  <div
    class="rounded-xl border p-4"
    :class="{
      'border-brand-200 bg-brand-50/60': (tone ?? 'brand') === 'brand',
      'border-amber-200 bg-amber-50/60': tone === 'amber',
      'border-slate-200 bg-slate-50': tone === 'slate',
    }"
  >
    <p v-if="title" class="flex items-center gap-2 font-display text-sm font-semibold text-slate-900">
      <GuideIcon v-if="icon" :name="icon" :size="16" />
      {{ title }}
    </p>
    <div class="text-sm leading-6 text-slate-700" :class="title ? 'mt-1.5' : ''">
      <slot />
    </div>
  </div>
</template>
