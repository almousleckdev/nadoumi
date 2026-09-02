<script setup lang="ts">
import type { Page, UniversitySummary } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('catalog.universitiesTitle'), t('catalog.universitiesSubtitle'))

const { publicGet } = useApi()
const { data } = await useAsyncData('universities', () =>
  publicGet<Page<UniversitySummary>>('universities').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])

function place(u: UniversitySummary): string {
  return [u.city, u.province, u.country].filter(Boolean).join(', ')
}
function typeLabel(type: UniversitySummary['type']): string {
  return type === 'PUBLIC' ? 'Public' : type === 'PRIVATE' ? 'Private' : ''
}
</script>

<template>
  <div>
    <PageHero :title="t('catalog.universitiesTitle')" :subtitle="t('catalog.universitiesSubtitle')" />
    <NContainer>
      <div v-if="items.length" class="grid gap-4 py-8 sm:grid-cols-2 lg:grid-cols-3">
        <NuxtLink
          v-for="u in items"
          :key="u.id"
          :to="localePath(`/universities/${u.id}`)"
          class="block rounded-xl border border-slate-200 p-5 no-underline transition-colors hover:border-brand-500"
        >
          <div class="flex items-start justify-between gap-2">
            <h3 class="font-display text-base font-semibold text-slate-900">{{ u.name }}</h3>
            <span
              v-if="u.featured"
              class="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800"
            >{{ t('catalog.featured') }}</span>
          </div>
          <p v-if="u.nameCn" class="mt-0.5 text-sm text-slate-500">{{ u.nameCn }}</p>
          <p class="mt-2 text-sm text-slate-600">{{ place(u) }}</p>
          <p v-if="typeLabel(u.type)" class="mt-1 text-xs uppercase tracking-wide text-slate-400">
            {{ typeLabel(u.type) }}
          </p>
        </NuxtLink>
      </div>
      <div v-else class="max-w-xl py-16">
        <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('catalog.empty') }}</h2>
        <p class="mt-2 text-slate-600">{{ t('catalog.emptyDetail') }}</p>
      </div>
    </NContainer>
  </div>
</template>
