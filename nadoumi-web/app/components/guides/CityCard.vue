<script setup lang="ts">
import type { GuideCity } from '~/data/guides/china'
import type { RegionTint } from '~/data/guides/regions'

defineProps<{ city: GuideCity, tint: RegionTint }>()
</script>

<template>
  <div class="rounded-xl border border-slate-200 bg-white p-4">
    <div class="flex items-baseline justify-between gap-2">
      <p class="font-display font-semibold text-slate-900">{{ city.name }}</p>
      <span class="font-display text-lg leading-none text-slate-300">{{ city.cn }}</span>
    </div>
    <p class="mt-1 text-xs leading-5 text-slate-500">{{ city.note }}</p>

    <p class="mt-3 text-[11px] font-semibold uppercase tracking-wide text-slate-400">Top universities</p>
    <ul class="mt-1.5 space-y-1.5">
      <li
        v-for="u in city.universities"
        :key="u.name"
        class="flex items-start gap-1.5 text-xs text-slate-700"
      >
        <GuideIcon name="award" :size="13" class="mt-0.5 shrink-0" :style="{ color: tint.fg }" />
        <span>{{ u.name }}<span v-if="u.cn" class="text-slate-400"> · {{ u.cn }}</span></span>
      </li>
    </ul>
  </div>
</template>
