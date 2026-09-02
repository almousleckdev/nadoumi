<script setup lang="ts">
import type { Page, ScholarshipSummary } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('catalog.scholarshipsTitle'), t('catalog.scholarshipsSubtitle'))

const { publicGet } = useApi()
// SSR-fetched for SEO. The endpoint lands with the Scholarship slice; until then
// this resolves to null and the page shows a designed "not yet" state.
const { data } = await useAsyncData('scholarships', () =>
  publicGet<Page<ScholarshipSummary>>('scholarships').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])

function meta(s: ScholarshipSummary): string {
  return [s.country, s.degreeLevel, s.field].filter(Boolean).join(' · ')
}
</script>

<template>
  <div>
    <PageHero :title="t('catalog.scholarshipsTitle')" :subtitle="t('catalog.scholarshipsSubtitle')" />
    <NContainer>
      <div v-if="items.length" class="grid gap-4 py-8 sm:grid-cols-2 lg:grid-cols-3">
        <ContentCard
          v-for="s in items"
          :key="s.id"
          :title="s.title"
          :meta="meta(s)"
          :to="localePath(`/scholarships/${s.id}`)"
        >
          {{ s.deadline ? t('catalog.deadline', { date: s.deadline }) : t('catalog.rollingDeadline') }}
        </ContentCard>
      </div>
      <div v-else class="max-w-xl py-16">
        <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('catalog.empty') }}</h2>
        <p class="mt-2 text-slate-600">{{ t('catalog.emptyDetail') }}</p>
        <NuxtLink :to="localePath('/universities')" class="mt-4 inline-block text-sm text-brand-600 underline">
          {{ t('home.seeAll') }}
        </NuxtLink>
      </div>
    </NContainer>
  </div>
</template>
