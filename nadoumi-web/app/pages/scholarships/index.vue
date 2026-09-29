<script setup lang="ts">
import type { ActiveChip } from '~/composables/useDiscovery'
import type { ScholarshipCard, ScholarshipFacets } from '~/types/catalog'

const { t } = useI18n()
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
const searchText = ref(typeof filters.q === 'string' ? filters.q : '')
function submitSearch() {
  setFilter('q', searchText.value.trim() || undefined)
}

const FUNDING = ['FULLY', 'PARTIAL', 'SELF'] as const
const LANGUAGES = ['ENGLISH', 'CHINESE', 'BOTH'] as const
const fundingOptions = computed(() => [
  { value: '', label: t('scholarships.anyFunding') },
  ...FUNDING.map(v => ({ value: v, label: t(`scholarships.funding.${v}`) })),
])
const languageOptions = computed(() => [
  { value: '', label: t('scholarships.anyLanguage') },
  ...LANGUAGES.map(v => ({ value: v, label: t(`scholarships.lang.${v}`) })),
])

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
  return code ? t(`scholarships.lang.${code}`) : ''
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
            <CatalogSearchForm
              id="sch-search"
              v-model="searchText"
              :label="t('scholarships.searchLabel')"
              :placeholder="t('scholarships.searchPlaceholder')"
              @submit="submitSearch"
            />
          </template>

          <template #filters>
            <FilterTextInput
              :value="filters.country"
              :placeholder="t('catalog.country')"
              :maxlength="2"
              uppercase
              class="w-24"
              @commit="setFilter('country', $event)"
            />
            <FilterSelect
              :value="filters.funding"
              :label="t('scholarships.anyFunding')"
              :options="fundingOptions"
              @commit="setFilter('funding', $event)"
            />
            <FilterSelect
              :value="filters.language"
              :label="t('scholarships.anyLanguage')"
              :options="languageOptions"
              @commit="setFilter('language', $event)"
            />
            <FilterToggle
              :checked="filters.hasStipend === 'true'"
              :label="t('scholarships.withStipend')"
              @commit="setFilter('hasStipend', $event ? 'true' : undefined)"
            />
          </template>

          <template #sort>
            <FilterSelect
              :value="sort"
              :label="t('scholarships.sortLabel')"
              :options="sortOptions"
              @commit="setSort"
            />
          </template>

          <template #chips>
            <FilterChips :chips="chips" :clear-label="t('catalog.clearFilters')" @remove="removeChip" @clear="clearAll" />
          </template>
        </FilterBar>

        <ScholarshipSecondaryFilters
          :level="Array.isArray(filters.level) ? filters.level : []"
          :category="Array.isArray(filters.category) ? filters.category : []"
          :facets="facets"
          @toggle="toggleValue"
        />

        <ScholarshipResultsTable
          :items="items"
          :pending="pending"
          :error="error"
          :is-empty="isEmpty"
          :has-active-filters="hasActiveFilters"
          @refresh="refresh"
          @clear="clearAll"
        />

        <Pagination :page="page" :page-count="pageCount" @update:page="setPage" />
      </div>
    </NContainer>
  </div>
</template>
