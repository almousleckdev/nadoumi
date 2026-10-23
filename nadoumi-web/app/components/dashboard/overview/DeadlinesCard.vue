<script setup lang="ts">
import type { ScholarshipCard as ScholarshipCardDto } from '~/types/catalog'

defineProps<{ items: ScholarshipCardDto[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.deadlines')">
    <template #actions>
      <NTextLink :to="localePath('/scholarships')">
        {{ t('dashboard.home.viewAll') }}
      </NTextLink>
    </template>
    <div v-if="pending" class="grid gap-2">
      <NSkeleton class="h-5 w-full rounded" />
      <NSkeleton class="h-5 w-full rounded" />
    </div>
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <p v-else-if="!items.length" class="py-2 text-sm text-slate-500">{{ t('dashboard.home.deadlinesEmpty') }}</p>
    <ul v-else class="grid gap-3">
      <li v-for="s in items" :key="s.id">
        <NuxtLink :to="localePath(`/scholarships/${s.slug}`)" class="flex items-center justify-between gap-2 text-sm hover:underline">
          <span class="min-w-0 truncate text-slate-700">{{ s.title }}</span>
          <span
            class="shrink-0 font-medium"
            :class="{
              'font-semibold text-slate-900': deadlineTone(daysUntilDeadline(s.deadline)) !== 'ok',
              'text-slate-500': deadlineTone(daysUntilDeadline(s.deadline)) === 'ok',
            }"
          >{{ t('catalog.deadline', { date: s.deadline }) }}</span>
        </NuxtLink>
      </li>
    </ul>
  </SectionCard>
</template>
