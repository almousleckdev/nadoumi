<script setup lang="ts">
import type { Page, ProgramCard, ScholarshipCard } from '~/types/catalog'
import { PROGRAM_TABS, SCHOLARSHIP_TABS } from '~/data/homeShowcase'

const props = defineProps<{ kind: 'scholarship' | 'program' }>()

const CAROUSEL_SIZE = 12

const SOURCES = {
  scholarship: { resource: 'scholarships', catalogPath: '/scholarships', tabs: SCHOLARSHIP_TABS, copy: 'scholarships', viewAll: 'scholarships.viewAll' },
  program: { resource: 'programs', catalogPath: '/programs', tabs: PROGRAM_TABS, copy: 'programs', viewAll: 'program.viewAll' },
} as const

const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet } = useApi()

const source = SOURCES[props.kind]
const active = ref<string>(source.tabs[0].key)
const activeTab = computed(() => source.tabs.find(tab => tab.key === active.value) ?? source.tabs[0])

const { data, pending, error, refresh } = useLazyAsyncData(
  `home-showcase-${props.kind}`,
  () => publicGet<Page<ScholarshipCard | ProgramCard>>(source.resource, { ...activeTab.value.query, size: CAROUSEL_SIZE }),
  { default: () => null },
)

function select(key: string) {
  if (key === active.value) return
  active.value = key
  refresh()
}

const items = computed(() => data.value?.content ?? [])
const viewAllTo = computed(() => {
  const qs = activeTab.value.viewAll
  return localePath(qs ? `${source.catalogPath}?${qs}` : source.catalogPath)
})
const copy = (key: string) => t(`home.showcase.${source.copy}.${key}`)
</script>

<template>
  <DiscoverySection
    :eyebrow="copy('eyebrow')"
    :title="copy('title')"
    :description="copy('description')"
    :carousel-label="copy('title')"
    :view-all-to="viewAllTo"
    :view-all-label="t(source.viewAll)"
    :pending="pending"
    :error="error ? t('errors.loadSection') : ''"
    :empty="!items.length"
    :empty-text="copy('empty')"
  >
    <template #tabs>
      <div
        role="group"
        :aria-label="t('home.showcase.tablistLabel')"
        class="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0"
      >
        <button
          v-for="tab in source.tabs"
          :key="tab.key"
          type="button"
          :aria-pressed="tab.key === active"
          class="shrink-0 whitespace-nowrap rounded-full border px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
          :class="tab.key === active
            ? 'border-brand-600 bg-brand-600 text-white'
            : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'"
          @click="select(tab.key)"
        >
          {{ t(`home.showcase.tabs.${tab.key}`) }}
        </button>
      </div>
    </template>
    <template v-if="kind === 'scholarship'">
      <ScholarshipCard v-for="s in (items as ScholarshipCard[])" :key="s.id" :scholarship="s" variant="carousel" />
    </template>
    <template v-else>
      <ProgramCard v-for="p in (items as ProgramCard[])" :key="p.id" :program="p" variant="carousel" />
    </template>
  </DiscoverySection>
</template>
