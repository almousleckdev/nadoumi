<script setup lang="ts">
import type { Page, ProgramSummary } from '~/types/catalog'

useSeo('Programs', 'Degree programmes and intakes offered by our universities.')

const { publicGet } = useApi()
const { data, error } = await useAsyncData('programs', () =>
  publicGet<Page<ProgramSummary>>('programs').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])
</script>

<template>
  <div>
    <PageHero title="Programs" subtitle="Bachelor, master and doctoral programmes by field and language." />
    <p v-if="error" class="nad-note">The programme catalogue is not available yet.</p>
    <div v-else-if="items.length" class="nad-grid">
      <ContentCard
        v-for="p in items"
        :key="p.id"
        :title="p.name"
        :meta="[p.degreeLevel, p.field, p.language].filter(Boolean).join(' · ')"
        :to="`/programs/${p.id}`"
      >
        View details
      </ContentCard>
    </div>
    <p v-else class="nad-note">No programmes published yet.</p>
  </div>
</template>

<style scoped>
.nad-note { color: var(--nad-muted); }
</style>
