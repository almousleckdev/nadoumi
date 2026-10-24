<script setup lang="ts">
import type { ScholarshipCard as ScholarshipCardDto } from '~/types/catalog'

defineProps<{ items: ScholarshipCardDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()

/** Bars show time left on a 90 day scale, so the nearest deadline reads as the shortest bar. */
const SCALE_DAYS = 90
const daysLeft = (deadline: string | null | undefined) => daysUntilDeadline(deadline) ?? SCALE_DAYS
const barWidth = (deadline: string | null | undefined) => `${Math.max(4, Math.min(100, (daysLeft(deadline) / SCALE_DAYS) * 100))}%`
</script>

<template>
  <SectionCard :title="t('dashboard.home.deadlines')">
    <template #actions>
      <NTextLink :to="localePath('/scholarships')">{{ t('dashboard.home.viewAll') }}</NTextLink>
    </template>
    <div v-if="pending" class="grid gap-2">
      <NSkeleton class="h-5 w-full rounded" />
      <NSkeleton class="h-5 w-full rounded" />
    </div>
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <p v-else-if="!items.length" class="py-2 text-sm text-slate-500">{{ t('dashboard.home.deadlinesEmpty') }}</p>
    <ul v-else class="grid gap-4">
      <li v-for="s in items" :key="s.id">
        <NuxtLink :to="localePath(`/scholarships/${s.slug}`)" class="block text-sm hover:underline">
          <span class="flex items-baseline justify-between gap-3">
            <span class="min-w-0 truncate text-slate-700">{{ s.title }}</span>
            <span class="shrink-0 text-xs font-semibold tabular-nums text-slate-900">{{ t('dashboard.home.daysLeft', { n: daysLeft(s.deadline) }) }}</span>
          </span>
          <span class="mt-1.5 block h-1.5 rounded-full bg-slate-100" aria-hidden="true">
            <span class="block h-1.5 rounded-full bg-slate-700" :style="{ width: barWidth(s.deadline) }" data-test="deadline-bar" />
          </span>
        </NuxtLink>
      </li>
    </ul>
  </SectionCard>
</template>
