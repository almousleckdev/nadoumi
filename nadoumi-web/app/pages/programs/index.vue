<script setup lang="ts">
import type { ProgramCard } from '~/types/catalog'
import type { ActiveChip } from '~/composables/useDiscovery'

const { t } = useI18n()
useSeo(t('catalog.programsTitle'), t('catalog.programsSubtitle'))

const {
  filters, page, pageCount, items, total,
  pending, error, isEmpty, hasActiveFilters,
  setFilter, setPage, clearFilters, refresh,
} = useDiscovery<ProgramCard>({
  resource: 'programs',
  filterKeys: ['q', 'field', 'featured'],
  pageSize: 12,
})

// local search box: committed on submit, not per keystroke
const searchText = ref(typeof filters.q === 'string' ? filters.q : '')
function submitSearch() {
  setFilter('q', searchText.value.trim() || undefined)
}

const chips = computed<ActiveChip[]>(() => {
  const out: ActiveChip[] = []
  if (typeof filters.q === 'string' && filters.q) out.push({ key: 'q', value: filters.q, label: `“${filters.q}”` })
  if (typeof filters.field === 'string' && filters.field) out.push({ key: 'field', value: filters.field, label: filters.field })
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
    <PageHero :title="t('catalog.programsTitle')" :subtitle="t('catalog.programsSubtitle')" />

    <NContainer>
      <div class="space-y-6 py-8">
        <FilterBar
          :count="total"
          :results-pending="pending"
          :count-label="t('catalog.resultCount', { n: total })"
        >
          <template #search>
            <CatalogSearchForm
              id="program-search"
              v-model="searchText"
              :label="t('catalog.searchPrograms')"
              :placeholder="t('catalog.searchProgramsPlaceholder')"
              @submit="submitSearch"
            />
          </template>

          <template #filters>
            <FilterTextInput
              :value="filters.field"
              :placeholder="t('program.field')"
              class="w-40"
              @commit="setFilter('field', $event)"
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
              <p class="font-display text-lg font-semibold text-slate-900">{{ t('catalog.noProgramsTitle') }}</p>
              <p class="mt-1 text-sm text-slate-600">
                {{ hasActiveFilters ? t('catalog.noResultsBody') : t('catalog.noProgramsBody') }}
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

          <ProgramCard v-for="p in items" :key="p.id" :program="p" />
        </ResultGrid>

        <Pagination :page="page" :page-count="pageCount" @update:page="setPage" />
      </div>
    </NContainer>
  </div>
</template>
