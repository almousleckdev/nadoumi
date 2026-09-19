<script setup lang="ts">
import type { UniversitySummary } from '~/types/catalog'
import type { ActiveChip } from '~/composables/useDiscovery'

const { t } = useI18n()
useSeo(t('catalog.universitiesTitle'), t('catalog.universitiesSubtitle'))

const {
  filters, page, pageCount, items, total,
  pending, error, isEmpty, hasActiveFilters,
  setFilter, setPage, clearFilters, refresh,
} = useDiscovery<UniversitySummary>({
  resource: 'universities',
  filterKeys: ['q', 'country', 'province', 'city', 'type', 'featured', 'recommended'],
  pageSize: 12,
})

// local search box — committed on submit, not per keystroke
const searchText = ref(typeof filters.q === 'string' ? filters.q : '')
function submitSearch() {
  setFilter('q', searchText.value.trim() || undefined)
}

const typeOptions = computed(() => [
  { value: '', label: t('catalog.anyType') },
  { value: 'PUBLIC', label: t('catalog.typePublic') },
  { value: 'PRIVATE', label: t('catalog.typePrivate') },
])

const chips = computed<ActiveChip[]>(() => {
  const out: ActiveChip[] = []
  if (typeof filters.q === 'string' && filters.q) out.push({ key: 'q', value: filters.q, label: `“${filters.q}”` })
  if (typeof filters.country === 'string' && filters.country) out.push({ key: 'country', value: filters.country, label: `${t('catalog.country')}: ${filters.country.toUpperCase()}` })
  if (typeof filters.province === 'string' && filters.province) out.push({ key: 'province', value: filters.province, label: `${t('catalog.province')}: ${filters.province}` })
  if (typeof filters.city === 'string' && filters.city) out.push({ key: 'city', value: filters.city, label: `${t('catalog.city')}: ${filters.city}` })
  if (filters.type === 'PUBLIC' || filters.type === 'PRIVATE') out.push({ key: 'type', value: filters.type, label: filters.type === 'PUBLIC' ? t('catalog.typePublic') : t('catalog.typePrivate') })
  if (filters.featured === 'true') out.push({ key: 'featured', value: 'true', label: t('catalog.featured') })
  return out
})
function removeChip(chip: ActiveChip) {
  setFilter(chip.key, undefined)
  if (chip.key === 'q') searchText.value = ''
}
function clearAll() {
  clearFilters()
  searchText.value = ''
}
</script>

<template>
  <div>
    <PageHero :title="t('catalog.universitiesTitle')" :subtitle="t('catalog.universitiesSubtitle')" />

    <NContainer>
      <div class="space-y-6 py-8">
        <FilterBar
          :count="total"
          :results-pending="pending"
          :count-label="t('catalog.resultCount', { n: total })"
        >
          <template #search>
            <CatalogSearchForm
              id="uni-search"
              v-model="searchText"
              :label="t('catalog.searchUniversities')"
              :placeholder="t('catalog.searchUniversitiesPlaceholder')"
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
            <FilterTextInput
              :value="filters.province"
              :placeholder="t('catalog.province')"
              class="w-32"
              @commit="setFilter('province', $event)"
            />
            <FilterTextInput
              :value="filters.city"
              :placeholder="t('catalog.city')"
              class="w-32"
              @commit="setFilter('city', $event)"
            />
            <FilterSelect
              :value="filters.type"
              :label="t('catalog.anyType')"
              :options="typeOptions"
              @commit="setFilter('type', $event)"
            />
            <FilterToggle
              :checked="filters.featured === 'true'"
              :label="t('catalog.featured')"
              @commit="setFilter('featured', $event ? 'true' : undefined)"
            />
          </template>

          <template #chips>
            <FilterChips :chips="chips" :clear-label="t('catalog.clearFilters')" @remove="removeChip" @clear="clearAll" />
          </template>
        </FilterBar>

        <ResultGrid :pending="pending" :error="Boolean(error)" :is-empty="isEmpty" @retry="refresh">
          <template #empty>
            <div class="rounded-xl border border-dashed border-slate-200 px-6 py-14 text-center">
              <p class="font-display text-lg font-semibold text-slate-900">{{ t('catalog.noUniversitiesTitle') }}</p>
              <p class="mt-1 text-sm text-slate-600">
                {{ hasActiveFilters ? t('catalog.noResultsBody') : t('catalog.noUniversitiesBody') }}
              </p>
              <button
                v-if="hasActiveFilters"
                type="button"
                class="mt-4 text-sm font-medium text-brand-700 hover:text-brand-800"
                @click="clearAll"
              >
                {{ t('catalog.clearFilters') }}
              </button>
            </div>
          </template>

          <UniversityCard v-for="u in items" :key="u.id" :university="u" />
        </ResultGrid>

        <Pagination :page="page" :page-count="pageCount" @update:page="setPage" />
      </div>
    </NContainer>
  </div>
</template>
