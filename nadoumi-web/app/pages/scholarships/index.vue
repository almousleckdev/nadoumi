<script setup lang="ts">
import type { Page, ScholarshipSummary } from '~/types/catalog'

useSeo('Scholarships', 'Browse scholarships by country, field and degree level.')

const { publicGet } = useApi()
// SSR-fetched for SEO. Endpoint lands with the Scholarship slice; empty until then.
const { data, error } = await useAsyncData('scholarships', () =>
  publicGet<Page<ScholarshipSummary>>('scholarships').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])
</script>

<template>
  <div>
    <PageHero title="Scholarships" subtitle="Funding opportunities across our destination countries." />
    <p v-if="error" class="nad-note">The scholarship catalogue is not available yet.</p>
    <div v-else-if="items.length" class="nad-grid">
      <ContentCard
        v-for="s in items"
        :key="s.id"
        :title="s.title"
        :meta="[s.country, s.degreeLevel, s.field].filter(Boolean).join(' · ')"
        :to="`/scholarships/${s.id}`"
      >
        {{ s.deadline ? `Deadline ${s.deadline}` : 'Rolling deadline' }}
      </ContentCard>
    </div>
    <p v-else class="nad-note">No scholarships published yet.</p>
  </div>
</template>

<style scoped>
.nad-note { color: var(--nad-muted); }
</style>
