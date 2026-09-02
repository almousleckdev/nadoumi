<script setup lang="ts">
import type { Page, UniversitySummary } from '~/types/catalog'

useSeo('Universities', 'Explore universities in our catalogue.')

const { publicGet } = useApi()
const { data, error } = await useAsyncData('universities', () =>
  publicGet<Page<UniversitySummary>>('universities').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])
</script>

<template>
  <div>
    <PageHero title="Universities" subtitle="Institutions across China, Malaysia and other destinations." />
    <p v-if="error" class="nad-note">The university catalogue is not available yet.</p>
    <div v-else-if="items.length" class="nad-grid">
      <ContentCard
        v-for="u in items"
        :key="u.id"
        :title="u.name"
        :meta="[u.city, u.country].filter(Boolean).join(', ')"
        :to="`/universities/${u.id}`"
      >
        View programmes
      </ContentCard>
    </div>
    <p v-else class="nad-note">No universities published yet.</p>
  </div>
</template>

<style scoped>
.nad-note { color: var(--nad-muted); }
</style>
