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
            <form role="search" class="flex items-center gap-2" @submit.prevent="submitSearch">
              <label for="uni-search" class="sr-only">{{ t('catalog.searchUniversities') }}</label>
              <input
                id="uni-search"
                v-model="searchText"
                type="search"
                :placeholder="t('catalog.searchUniversitiesPlaceholder')"
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
            <input
              :value="filters.province ?? ''"
              type="text"
              :placeholder="t('catalog.province')"
              class="w-32 rounded-md border border-slate-300 px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="setFilter('province', ($event.target as HTMLInputElement).value.trim() || undefined)"
            >
            <input
              :value="filters.city ?? ''"
              type="text"
              :placeholder="t('catalog.city')"
              class="w-32 rounded-md border border-slate-300 px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="setFilter('city', ($event.target as HTMLInputElement).value.trim() || undefined)"
            >
            <select
              :value="filters.type ?? ''"
              class="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              :aria-label="t('catalog.anyType')"
              @change="setFilter('type', ($event.target as HTMLSelectElement).value || undefined)"
            >
              <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
            <label class="inline-flex items-center gap-1.5 text-sm text-slate-700">
              <input
                type="checkbox"
                :checked="filters.featured === 'true'"
                class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
                @change="setFilter('featured', ($event.target as HTMLInputElement).checked ? 'true' : undefined)"
              >
              {{ t('catalog.featured') }}
            </label>
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
