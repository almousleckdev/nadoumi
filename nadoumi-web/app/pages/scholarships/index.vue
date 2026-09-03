<script setup lang="ts">
import type { ActiveChip } from '~/composables/useDiscovery'
import type { ScholarshipCard, ScholarshipFacets } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('catalog.scholarshipsTitle'), t('catalog.scholarshipsSubtitle'))

const { publicGet } = useApi()

const {
  filters, sort, page, pageCount, items, total,
  pending, error, isEmpty, hasActiveFilters,
  setFilter, toggleValue, setSort, setPage, clearFilters, refresh,
} = useDiscovery<ScholarshipCard>({
  resource: 'scholarships',
  filterKeys: ['q', 'country', 'funding', 'language', 'hasStipend', 'level', 'category'],
  arrayKeys: ['level', 'category'],
  pageSize: 12,
})

// live facet counts for the current filter set
const facetQuery = computed(() => {
  const q: Record<string, string> = {}
  for (const [k, v] of Object.entries(filters)) {
    if (v == null || (Array.isArray(v) && !v.length) || v === '') continue
    q[k] = Array.isArray(v) ? v.join(',') : String(v)
  }
  return q
})
const { data: facets } = useAsyncData<ScholarshipFacets | null>(
  () => `scholarship-facets:${JSON.stringify(facetQuery.value)}`,
  () => publicGet<ScholarshipFacets>('scholarships/facets', facetQuery.value).catch(() => null),
  { watch: [facetQuery], default: () => null },
)
function count(dim: keyof ScholarshipFacets, value: string): number {
  return facets.value?.[dim].find(b => b.value === value)?.count ?? 0
}

const LEVELS = ['NON_DEGREE', 'DIPLOMA', 'BACHELOR', 'MASTER', 'PHD'] as const
const CATEGORIES = ['CSC', 'CGS', 'GOVERNMENT', 'PROVINCIAL', 'UNIVERSITY', 'PRESIDENTIAL', 'LANGUAGE', 'TYPE_A', 'TYPE_B', 'TYPE_C', 'TYPE_D'] as const

const searchText = ref(typeof filters.q === 'string' ? filters.q : '')
function submitSearch() {
  setFilter('q', searchText.value.trim() || undefined)
}

const sortOptions = computed(() => [
  { value: '', label: t('scholarships.sortRelevance') },
  { value: 'deadline', label: t('scholarships.sortDeadline') },
  { value: 'newest', label: t('scholarships.sortNewest') },
  { value: 'title', label: t('scholarships.sortTitle') },
])

function levelLabel(code: string) {
  return t(`scholarships.level.${code}`)
}
function fundingLabel(model: string) {
  return t(`scholarships.funding.${model}`)
}
function langLabel(code?: string | null) {
  return code ? t(`scholarships.lang.${code}`) : '—'
}
function nextIntake(s: ScholarshipCard) {
  return s.intakes[0] ? t(`scholarships.intake.${s.intakes[0].term}`, s.intakes[0].term) : '—'
}
function place(s: ScholarshipCard) {
  return [s.city, s.province, s.country].filter(Boolean).join(', ')
}

const chips = computed<ActiveChip[]>(() => {
  const out: ActiveChip[] = []
  if (typeof filters.q === 'string' && filters.q) out.push({ key: 'q', value: filters.q, label: `“${filters.q}”` })
  if (typeof filters.country === 'string' && filters.country) out.push({ key: 'country', value: filters.country, label: filters.country.toUpperCase() })
  if (typeof filters.funding === 'string' && filters.funding) out.push({ key: 'funding', value: filters.funding, label: fundingLabel(filters.funding) })
  if (typeof filters.language === 'string' && filters.language) out.push({ key: 'language', value: filters.language, label: langLabel(filters.language) })
  if (filters.hasStipend === 'true') out.push({ key: 'hasStipend', value: 'true', label: t('scholarships.withStipend') })
  for (const lv of (Array.isArray(filters.level) ? filters.level : [])) out.push({ key: 'level', value: lv, label: levelLabel(lv) })
  for (const c of (Array.isArray(filters.category) ? filters.category : [])) out.push({ key: 'category', value: c, label: t(`scholarships.category.${c}`, c) })
  return out
})
function removeChip(chip: ActiveChip) {
  if (chip.key === 'level' || chip.key === 'category') toggleValue(chip.key, chip.value)
  else setFilter(chip.key, undefined)
  if (chip.key === 'q') searchText.value = ''
}
function clearAll() {
  clearFilters()
  searchText.value = ''
}
</script>

<template>
  <div>
    <PageHero :title="t('catalog.scholarshipsTitle')" :subtitle="t('catalog.scholarshipsSubtitle')" />

    <NContainer>
      <div class="space-y-6 py-8">
        <FilterBar
          :count="total"
          :results-pending="pending"
          :count-label="t('catalog.resultCount', { n: total })"
        >
          <template #search>
            <form role="search" class="flex items-center gap-2" @submit.prevent="submitSearch">
              <label for="sch-search" class="sr-only">{{ t('scholarships.searchLabel') }}</label>
              <input
                id="sch-search"
                v-model="searchText"
                type="search"
                :placeholder="t('scholarships.searchPlaceholder')"
                class="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus-visible:border-brand-500"
              >
              <NButton type="submit" size="sm">{{ t('common.search') }}</NButton>
            </form>
          </template>

          <template #filters>
            <input
              :value="filters.country ?? ''"
              type="text"
              maxlength="2"
              :placeholder="t('catalog.country')"
              class="w-24 rounded-md border border-slate-300 px-2.5 py-2 text-sm uppercase outline-none focus-visible:border-brand-500"
              @change="setFilter('country', ($event.target as HTMLInputElement).value.trim().toUpperCase() || undefined)"
            >
            <select
              :value="filters.funding ?? ''"
              :aria-label="t('scholarships.anyFunding')"
              class="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="setFilter('funding', ($event.target as HTMLSelectElement).value || undefined)"
            >
              <option value="">{{ t('scholarships.anyFunding') }}</option>
              <option value="FULLY">{{ t('scholarships.funding.FULLY') }}</option>
              <option value="PARTIAL">{{ t('scholarships.funding.PARTIAL') }}</option>
              <option value="SELF">{{ t('scholarships.funding.SELF') }}</option>
            </select>
            <select
              :value="filters.language ?? ''"
              :aria-label="t('scholarships.anyLanguage')"
              class="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="setFilter('language', ($event.target as HTMLSelectElement).value || undefined)"
            >
              <option value="">{{ t('scholarships.anyLanguage') }}</option>
              <option value="ENGLISH">{{ t('scholarships.lang.ENGLISH') }}</option>
              <option value="CHINESE">{{ t('scholarships.lang.CHINESE') }}</option>
              <option value="BOTH">{{ t('scholarships.lang.BOTH') }}</option>
            </select>
            <label class="inline-flex items-center gap-1.5 text-sm text-slate-700">
              <input
                type="checkbox"
                :checked="filters.hasStipend === 'true'"
                class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
                @change="setFilter('hasStipend', ($event.target as HTMLInputElement).checked ? 'true' : undefined)"
              >
              {{ t('scholarships.withStipend') }}
            </label>
          </template>

          <template #sort>
            <select
              :value="sort"
              :aria-label="t('scholarships.sortLabel')"
              class="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="setSort(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="o in sortOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </template>

          <template #chips>
            <FilterChips :chips="chips" :clear-label="t('catalog.clearFilters')" @remove="removeChip" @clear="clearAll" />
          </template>
        </FilterBar>

        <!-- secondary filters: level + category with live counts -->
        <details class="rounded-xl border border-slate-200 bg-white p-4">
          <summary class="cursor-pointer text-sm font-medium text-slate-700">{{ t('scholarships.moreFilters') }}</summary>
          <div class="mt-4 grid gap-6 sm:grid-cols-2">
            <fieldset>
              <legend class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.fLevel') }}</legend>
              <div class="mt-2 flex flex-wrap gap-x-4 gap-y-2">
                <label v-for="lv in LEVELS" :key="lv" class="inline-flex items-center gap-1.5 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    :checked="Array.isArray(filters.level) && filters.level.includes(lv)"
                    class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
                    @change="toggleValue('level', lv)"
                  >
                  {{ levelLabel(lv) }}
                  <span class="text-slate-400">({{ count('levels', lv) }})</span>
                </label>
              </div>
            </fieldset>
            <fieldset>
              <legend class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.fCategory') }}</legend>
              <div class="mt-2 flex flex-wrap gap-x-4 gap-y-2">
                <label v-for="c in CATEGORIES" :key="c" class="inline-flex items-center gap-1.5 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    :checked="Array.isArray(filters.category) && filters.category.includes(c)"
                    class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
                    @change="toggleValue('category', c)"
                  >
                  {{ t(`scholarships.category.${c}`, c) }}
                  <span class="text-slate-400">({{ count('categories', c) }})</span>
                </label>
              </div>
            </fieldset>
          </div>
        </details>

        <!-- results table -->
        <div class="overflow-x-auto rounded-xl border border-slate-200">
          <table class="w-full min-w-[52rem] text-left text-sm">
            <thead class="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colTitle') }}</th>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colLevel') }}</th>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colLanguage') }}</th>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colIntake') }}</th>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colDeadline') }}</th>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colFunding') }}</th>
                <th class="px-4 py-3 font-medium">{{ t('scholarships.colLocation') }}</th>
                <th class="px-4 py-3" />
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-if="pending">
                <td colspan="8" class="px-4 py-10 text-center text-slate-400">{{ t('common.loading') }}…</td>
              </tr>
              <tr v-else-if="error">
                <td colspan="8" class="px-4 py-8 text-center">
                  <NAlert tone="danger">
                    {{ t('errors.loadSection') }}
                    <button type="button" class="ml-2 font-medium underline" @click="refresh">{{ t('common.retry') }}</button>
                  </NAlert>
                </td>
              </tr>
              <tr v-else-if="isEmpty">
                <td colspan="8" class="px-4 py-12 text-center">
                  <p class="font-display text-base font-semibold text-slate-900">
                    {{ hasActiveFilters ? t('catalog.noResultsTitle') : t('scholarships.noneTitle') }}
                  </p>
                  <p class="mt-1 text-slate-600">
                    {{ hasActiveFilters ? t('catalog.noResultsBody') : t('scholarships.noneBody') }}
                  </p>
                  <button v-if="hasActiveFilters" type="button" class="mt-3 text-sm font-medium text-brand-700" @click="clearAll">
                    {{ t('catalog.clearFilters') }}
                  </button>
                </td>
              </tr>
              <tr v-for="s in items" v-else :key="s.id" class="hover:bg-slate-50/60">
                <td class="px-4 py-3">
                  <NuxtLink :to="localePath(`/scholarships/${s.slug}`)" class="font-medium text-slate-900 hover:text-brand-800">
                    {{ s.title }}
                  </NuxtLink>
                  <span v-if="s.hot" class="ml-2 rounded-full bg-amber-100 px-1.5 py-0.5 text-[0.65rem] font-semibold text-amber-800">{{ t('scholarships.hot') }}</span>
                  <span v-else-if="s.featured" class="ml-2 rounded-full bg-brand-100 px-1.5 py-0.5 text-[0.65rem] font-semibold text-brand-800">{{ t('catalog.featured') }}</span>
                </td>
                <td class="px-4 py-3 text-slate-600">{{ s.levels.map(levelLabel).join(', ') || '—' }}</td>
                <td class="px-4 py-3 text-slate-600">{{ langLabel(s.teachingLanguage) }}</td>
                <td class="px-4 py-3 text-slate-600">{{ nextIntake(s) }}</td>
                <td class="px-4 py-3 text-slate-600">{{ s.deadline ?? t('catalog.rollingDeadline') }}</td>
                <td class="px-4 py-3">
                  <span class="text-slate-700">{{ fundingLabel(s.fundingModel) }}</span>
                  <span v-if="s.hasStipend" class="ml-1 text-brand-600" :title="t('scholarships.withStipend')">•</span>
                </td>
                <td class="px-4 py-3 text-slate-600">{{ place(s) }}</td>
                <td class="px-4 py-3 text-right">
                  <NButton :to="localePath(`/scholarships/${s.slug}`)" size="sm">{{ t('scholarships.apply') }}</NButton>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <Pagination :page="page" :page-count="pageCount" @update:page="setPage" />
      </div>
    </NContainer>
  </div>
</template>
