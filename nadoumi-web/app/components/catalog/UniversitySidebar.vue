<script setup lang="ts">
import type { UniversityDetail } from '~/types/catalog'

defineProps<{ university: UniversityDetail }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <aside class="space-y-6 lg:sticky lg:top-24 lg:self-start">
      <div v-if="university.rankings.length" class="rounded-xl border border-slate-200 bg-white p-5">
        <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('university.rankings') }}</h2>
        <ul class="mt-3 space-y-2 text-sm">
          <li v-for="r in university.rankings" :key="r.id" class="flex items-baseline justify-between gap-3">
            <span class="text-slate-700">{{ r.source }}<span v-if="r.rankYear" class="text-slate-400"> · {{ r.rankYear }}</span></span>
            <span class="font-display font-semibold text-slate-900">#{{ r.rankPosition }}</span>
          </li>
        </ul>
      </div>

      <div v-if="university.website || university.admissionsEmail || university.officePhone" class="rounded-xl border border-slate-200 bg-white p-5 text-sm">
        <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('university.contact') }}</h2>
        <ul class="mt-3 space-y-2 text-slate-700">
          <li v-if="university.website">
            <a :href="university.website" target="_blank" rel="noopener" class="text-brand-700 hover:text-brand-800">{{ t('university.website') }} ↗</a>
          </li>
          <li v-if="university.admissionsEmail">{{ t('university.admissionsEmail') }}: {{ university.admissionsEmail }}</li>
          <li v-if="university.officePhone">{{ t('university.officePhone') }}: {{ university.officePhone }}</li>
        </ul>
      </div>

      <NuxtLink :to="localePath('/universities')" class="inline-flex items-center gap-1.5 text-sm font-medium text-brand-700 hover:text-brand-800">
        <span aria-hidden="true">←</span> {{ t('catalog.allUniversities') }}
      </NuxtLink>
  </aside>
</template>
