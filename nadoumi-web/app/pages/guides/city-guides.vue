<script setup lang="ts">
import type { Division } from '~/data/guides/china'
import { CHINA_REGIONS, CHINA_DIVISION_COUNT } from '~/data/guides/china'
import { GUIDE_HEROES } from '~/data/guides/heroes'

const localePath = useLocalePath()

useSeo(
  'City Guides, studying across China',
  'Every province and region of China: history, culture, language and festivals, with two cities and their leading universities. Search by province, city or university.',
)

interface Entry {
  regionId: string
  regionName: string
  division: Division
}

const query = ref('')
const activeRegion = ref('all')

const regionChips = computed(() => [
  { id: 'all', name: 'All regions' },
  ...CHINA_REGIONS.map(r => ({ id: r.id, name: r.name })),
])

const allEntries = computed<Entry[]>(() =>
  CHINA_REGIONS.flatMap(r =>
    r.divisions.map(d => ({ regionId: r.id, regionName: r.name, division: d })),
  ),
)

function haystack(d: Division): string {
  return [
    d.name,
    d.cn,
    d.overview,
    d.culture,
    ...d.cities.flatMap(c => [
      c.name,
      c.cn,
      c.note,
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
            <NuxtLink :to="localePath('/guides')" class="no-underline hover:text-white">Guides</NuxtLink>
            <span class="mx-2" aria-hidden="true">/</span>
            <span>City Guides</span>
          </nav>
          <span class="mt-5 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 text-white ring-1 ring-white/20">
            <GuideIcon name="map" :size="22" />
          </span>
          <h1 class="mt-4 font-display text-4xl font-bold tracking-tight sm:text-5xl">City Guides</h1>
          <p class="mt-4 text-lg leading-8 text-slate-200">
            Every province and region of China, its history, culture, language and festivals, with two
            cities and their leading universities.
          </p>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="py-12 sm:py-16">
        <p class="max-w-2xl leading-7 text-slate-700">
          China is vast and varied. Each province has its own dialects, cuisine, festivals and
          character, and its own universities. Search by province, city or university, or filter by
          region, then explore the institutions on our
          <NuxtLink :to="localePath('/universities')" class="text-brand-700 underline">Universities</NuxtLink> page.
        </p>

        <!-- search + region filter -->
        <div class="sticky top-16 z-10 -mx-4 mt-8 border-y border-slate-200 bg-white/90 px-4 py-4 backdrop-blur sm:mx-0 sm:rounded-xl sm:border sm:px-5">
          <label for="city-search" class="sr-only">Search provinces, cities and universities</label>
          <NInput
            id="city-search"
            v-model="query"
            placeholder="Search a province, city or university"
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

        <p class="mt-6 text-sm text-slate-500">
          Showing {{ results.length }} of {{ CHINA_DIVISION_COUNT }} provinces and regions
        </p>

        <!-- results -->
        <div v-if="results.length" class="mt-4 grid gap-6 lg:grid-cols-2">
          <ProvinceCard
            v-for="e in results"
            :key="e.division.name"
            :division="e.division"
            :region-id="e.regionId"
          />
        </div>

        <div v-else class="mt-8 rounded-xl border border-dashed border-slate-300 p-10 text-center">
          <p class="font-display text-lg font-semibold text-slate-900">No matching province</p>
          <p class="mt-1 text-sm text-slate-500">Try a different city, university or region.</p>
          <button
            type="button"
            class="mt-4 rounded-md border border-slate-200 px-4 py-2 text-sm font-medium text-brand-700 hover:border-brand-300"
            @click="reset"
          >
            Clear search
          </button>
        </div>

        <p class="mt-12 border-t border-slate-200 pt-8 text-sm text-slate-500">
          University lists highlight a few leading institutions in each city and are not exhaustive.
          Administrative descriptions follow the People's Republic of China.
        </p>
      </div>
    </NContainer>
  </div>
</template>
