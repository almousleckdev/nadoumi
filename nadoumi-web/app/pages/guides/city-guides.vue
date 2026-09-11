<script setup lang="ts">
import type { Division } from '~/data/guides/china'
import { CHINA_REGIONS, CHINA_DIVISION_COUNT } from '~/data/guides/china'
import { GUIDE_HEROES } from '~/data/guides/heroes'

const { t } = useI18n()
const localePath = useLocalePath()

useSeo(t('guides.cityGuides.seoTitle'), t('guides.cityGuides.seoDescription'))

interface Entry {
  regionId: string
  regionName: string
  division: Division
}

const query = ref('')
const activeRegion = ref('all')

const regionChips = computed(() => [
  { id: 'all', name: t('guides.cityGuides.allRegions') },
  ...CHINA_REGIONS.map(r => ({ id: r.id, name: t(`guides.cityGuides.regions.${r.id}.name`) })),
])

const allEntries = computed<Entry[]>(() =>
  CHINA_REGIONS.flatMap(r =>
    r.divisions.map(d => ({ regionId: r.id, regionName: t(`guides.cityGuides.regions.${r.id}.name`), division: d })),
  ),
)

function haystack(d: Division): string {
  return [
    d.name,
    d.cn,
    t(`guides.cityGuides.divisions.${d.slug}.overview`),
    t(`guides.cityGuides.divisions.${d.slug}.culture`),
    ...d.cities.flatMap(c => [
      c.name,
      c.cn,
      t(`guides.cityGuides.divisions.${d.slug}.cities.${c.id}.note`),
      ...c.universities.flatMap(u => [u.name, u.cn ?? '']),
    ]),
  ]
    .join(' ')
    .toLowerCase()
}

const results = computed(() => {
  const q = query.value.trim().toLowerCase()
  return allEntries.value.filter((e: Entry) => {
    if (activeRegion.value !== 'all' && e.regionId !== activeRegion.value) return false
    if (!q) return true
    return haystack(e.division).includes(q)
  })
})

function reset() {
  query.value = ''
  activeRegion.value = 'all'
}
</script>

<template>
  <div>
    <section class="relative isolate flex min-h-[360px] items-end overflow-hidden bg-slate-900 text-white sm:min-h-[460px]">
      <img
        :src="GUIDE_HEROES['city-guides']"
        alt=""
        decoding="async"
        class="absolute inset-0 -z-10 h-full w-full object-cover"
      >
      <div class="absolute inset-0 -z-10 bg-gradient-to-t from-slate-950/90 via-slate-950/55 to-slate-950/15" aria-hidden="true" />
      <NContainer>
        <div class="max-w-2xl py-10 sm:py-12">
          <nav class="text-sm text-white/70">
            <NuxtLink :to="localePath('/guides')" class="no-underline hover:text-white">{{ t('guides.hub.title') }}</NuxtLink>
            <span class="mx-2" aria-hidden="true">/</span>
            <span>{{ t('guides.meta.city-guides.title') }}</span>
          </nav>
          <span class="mt-5 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 text-white ring-1 ring-white/20">
            <GuideIcon name="map" :size="22" />
          </span>
          <h1 class="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">{{ t('guides.meta.city-guides.title') }}</h1>
          <p class="mt-4 text-lg leading-8 text-slate-200">
            {{ t('guides.cityGuides.heroSubtitle') }}
          </p>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="py-12 sm:py-16">
        <i18n-t keypath="guides.cityGuides.intro" tag="p" scope="global" class="max-w-2xl leading-7 text-slate-700">
          <template #universities>
            <NuxtLink :to="localePath('/universities')" class="text-brand-700 underline">{{ t('nav.universities') }}</NuxtLink>
          </template>
        </i18n-t>

        <!-- search + region filter -->
        <div class="sticky top-16 z-10 -mx-4 mt-8 border-y border-slate-200 bg-white/90 px-4 py-4 backdrop-blur sm:mx-0 sm:rounded-xl sm:border sm:px-5">
          <label for="city-search" class="sr-only">{{ t('guides.cityGuides.searchLabel') }}</label>
          <NInput
            id="city-search"
            v-model="query"
            :placeholder="t('guides.cityGuides.searchPlaceholder')"
          >
            <template #suffix>
              <GuideIcon name="search" :size="18" class="text-slate-400" />
            </template>
          </NInput>

          <div class="mt-3 flex flex-wrap gap-2">
            <button
              v-for="chip in regionChips"
              :key="chip.id"
              type="button"
              class="rounded-full border px-3 py-1 text-sm transition"
              :class="activeRegion === chip.id
                ? 'border-brand-600 bg-brand-600 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:border-brand-300 hover:text-brand-700'"
              @click="activeRegion = chip.id"
            >
              {{ chip.name }}
            </button>
          </div>
        </div>

        <i18n-t keypath="guides.cityGuides.showingCount" tag="p" scope="global" class="mt-6 text-sm text-slate-500">
          <template #count>{{ results.length }}</template>
          <template #total>{{ CHINA_DIVISION_COUNT }}</template>
        </i18n-t>

        <!-- results -->
        <div v-if="results.length" class="mt-4 grid gap-6 lg:grid-cols-2">
          <ProvinceCard
            v-for="e in results"
            :key="e.division.slug"
            :division="e.division"
            :region-id="e.regionId"
          />
        </div>

        <div v-else class="mt-8 rounded-xl border border-dashed border-slate-300 p-10 text-center">
          <p class="font-display text-lg font-semibold text-slate-900">{{ t('guides.cityGuides.noResults.title') }}</p>
          <p class="mt-1 text-sm text-slate-500">{{ t('guides.cityGuides.noResults.body') }}</p>
          <button
            type="button"
            class="mt-4 rounded-md border border-slate-200 px-4 py-2 text-sm font-medium text-brand-700 hover:border-brand-300"
            @click="reset"
          >
            {{ t('guides.cityGuides.noResults.clear') }}
          </button>
        </div>

        <p class="mt-12 border-t border-slate-200 pt-8 text-sm text-slate-500">
          {{ t('guides.cityGuides.universitiesNote') }}
        </p>
      </div>
    </NContainer>
  </div>
</template>
