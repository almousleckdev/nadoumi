<script setup lang="ts">
import type { UniversitySummary } from '~/types/catalog'
import type { ActiveChip } from '~/composables/useDiscovery'

const { t } = useI18n()
useSeo(t('catalog.universitiesTitle'), t('catalog.universitiesSubtitle'))

const d = useDiscovery<UniversitySummary>({
  resource: 'universities',
  filterKeys: ['q', 'country', 'province', 'city', 'type', 'featured', 'recommended'],
  defaultSort: '',
  pageSize: 12,
})

// local-only search box (committed on submit, not per keystroke)
const searchText = ref(typeof d.filters.q === 'string' ? d.filters.q : '')
function submitSearch() {
  d.setFilter('q', searchText.value.trim() || undefined)
}

const typeOptions = [
  { value: '', label: t('catalog.anyType') },
  { value: 'PUBLIC', label: t('catalog.typePublic') },
  { value: 'PRIVATE', label: t('catalog.typePrivate') },
]

const chips = computed<ActiveChip[]>(() => {
  const out: ActiveChip[] = []
  const f = d.filters
  if (typeof f.country === 'string' && f.country) out.push({ key: 'country', value: f.country, label: `${t('catalog.country')}: ${f.country.toUpperCase()}` })
  if (typeof f.province === 'string' && f.province) out.push({ key: 'province', value: f.province, label: `${t('catalog.province')}: ${f.province}` })
  if (typeof f.city === 'string' && f.city) out.push({ key: 'city', value: f.city, label: `${t('catalog.city')}: ${f.city}` })
  if (f.type === 'PUBLIC' || f.type === 'PRIVATE') out.push({ key: 'type', value: f.type, label: f.type === 'PUBLIC' ? t('catalog.typePublic') : t('catalog.typePrivate') })
  if (f.featured === 'true') out.push({ key: 'featured', value: 'true', label: t('catalog.featured') })
  if (f.recommended === 'true') out.push({ key: 'recommended', value: 'true', label: t('catalog.recommendedShort') })
  return out
})
function removeChip(chip: ActiveChip) {
  d.setFilter(chip.key, undefined)
  if (chip.key === 'q') searchText.value = ''
}
function clearAll() {
  d.clearFilters()
  searchText.value = ''
}
</script>

<template>
  <div>
    <PageHero :title="t('catalog.universitiesTitle')" :subtitle="t('catalog.universitiesSubtitle')" />

    <NContainer>
      <div class="space-y-6 py-8">
        <FilterBar
          :count="d.total.value"
          :results-pending="d.pending.value"
          :count-label="t('catalog.resultCount', { n: d.total.value })"
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
              :value="d.filters.country ?? ''"
              type="text"
              maxlength="2"
              :placeholder="t('catalog.country')"
              class="w-24 rounded-md border border-slate-300 px-2.5 py-2 text-sm uppercase outline-none focus-visible:border-brand-500"
              @change="d.setFilter('country', ($event.target as HTMLInputElement).value.trim().toUpperCase() || undefined)"
            >
            <input
              :value="d.filters.province ?? ''"
              type="text"
              :placeholder="t('catalog.province')"
              class="w-32 rounded-md border border-slate-300 px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="d.setFilter('province', ($event.target as HTMLInputElement).value.trim() || undefined)"
            >
            <input
              :value="d.filters.city ?? ''"
              type="text"
              :placeholder="t('catalog.city')"
              class="w-32 rounded-md border border-slate-300 px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              @change="d.setFilter('city', ($event.target as HTMLInputElement).value.trim() || undefined)"
            >
            <select
              :value="d.filters.type ?? ''"
              class="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
              :aria-label="t('catalog.anyType')"
              @change="d.setFilter('type', ($event.target as HTMLSelectElement).value || undefined)"
            >
              <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
            <label class="inline-flex items-center gap-1.5 text-sm text-slate-700">
              <input
                type="checkbox"
                :checked="d.filters.featured === 'true'"
                class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
                @change="d.setFilter('featured', ($event.target as HTMLInputElement).checked ? 'true' : undefined)"
              >
              {{ t('catalog.featured') }}
            </label>
          </template>

          <template #chips>
            <FilterChips :chips="chips" :clear-label="t('catalog.clearFilters')" @remove="removeChip" @clear="clearAll" />
          </template>
        </FilterBar>

        <ResultGrid
          :pending="d.pending.value"
          :error="Boolean(d.error.value)"
          :is-empty="d.isEmpty.value"
          @retry="d.refresh"
        >
          <template #empty>
            <div class="rounded-xl border border-dashed border-slate-200 px-6 py-14 text-center">
              <p class="font-display text-lg font-semibold text-slate-900">{{ t('catalog.noUniversitiesTitle') }}</p>
              <p class="mt-1 text-sm text-slate-600">
                {{ d.hasActiveFilters.value ? t('catalog.noResultsBody') : t('catalog.noUniversitiesBody') }}
              </p>
              <button
                v-if="d.hasActiveFilters.value"
                type="button"
                class="mt-4 text-sm font-medium text-brand-700 hover:text-brand-800"
                @click="clearAll"
              >
                {{ t('catalog.clearFilters') }}
              </button>
            </div>
          </template>

          <UniversityCard
            v-for="u in d.items.value"
            :key="u.id"
            :university="u"
          />
        </ResultGrid>

        <Pagination
          :page="d.page.value"
          :page-count="d.pageCount.value"
          @update:page="d.setPage"
        />
      </div>
    </NContainer>
  </div>
</template>
