<script setup lang="ts">
import type { Page, ProgramCard, ScholarshipCard } from '~/types/catalog'
import type { HomeRail } from '~/data/homeShowcase'

const props = defineProps<{ rail: HomeRail }>()

const CAROUSEL_SIZE = 12

const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet } = useApi()

const isScholarship = props.rail.kind === 'scholarship'
const resource = isScholarship ? 'scholarships' : 'programs'
const catalogPath = isScholarship ? '/scholarships' : '/programs'

const { data, pending, error } = await useAsyncData(
  `home-rail-${props.rail.id}`,
  () => publicGet<Page<ScholarshipCard | ProgramCard>>(resource, { ...props.rail.query, size: CAROUSEL_SIZE }),
  { default: () => null },
)

const items = computed(() => data.value?.content ?? [])
const copy = (key: string) => t(`home.rails.${props.rail.id}.${key}`)
const viewAllTo = localePath(props.rail.viewAll ? `${catalogPath}?${props.rail.viewAll}` : catalogPath)
</script>

<template>
  <NContainer v-if="pending || error || items.length">
    <DiscoverySection
      :eyebrow="copy('eyebrow')"
      :title="copy('title')"
      :description="copy('description')"
      :carousel-label="copy('title')"
      :view-all-to="viewAllTo"
      :view-all-label="t(isScholarship ? 'scholarships.viewAll' : 'program.viewAll')"
      :pending="pending"
      :error="error ? t('errors.loadSection') : ''"
      :empty="false"
    >
      <template v-if="isScholarship">
        <ScholarshipCard v-for="s in (items as ScholarshipCard[])" :key="s.id" :scholarship="s" variant="carousel" />
      </template>
      <template v-else>
        <ProgramCard v-for="p in (items as ProgramCard[])" :key="p.id" :program="p" variant="carousel" />
      </template>
    </DiscoverySection>
  </NContainer>
</template>
