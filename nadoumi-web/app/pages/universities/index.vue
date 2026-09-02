<script setup lang="ts">
import type { Page, UniversitySummary } from '~/types/catalog'

useSeo('Universities', 'Explore universities and the programmes each one offers.')

const { publicGet } = useApi()
const { data, error } = await useAsyncData('universities', () =>
  publicGet<Page<UniversitySummary>>('universities').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])

function place(u: UniversitySummary): string {
  return [u.city, u.province, u.country].filter(Boolean).join(', ')
}
function typeLabel(t: UniversitySummary['type']): string {
  return t === 'PUBLIC' ? 'Public' : t === 'PRIVATE' ? 'Private' : ''
}
</script>

<template>
  <div>
    <PageHero
      title="Universities"
      subtitle="Institutions across China, Malaysia and other destinations — each with the programmes it offers."
    />
    <NContainer>
      <p v-if="error" class="py-10 text-slate-600">The university catalogue is not available yet.</p>
      <div v-else-if="items.length" class="grid gap-4 py-8 sm:grid-cols-2 lg:grid-cols-3">
        <NuxtLink
          v-for="u in items"
          :key="u.id"
          :to="`/universities/${u.id}`"
          class="block rounded-xl border border-slate-200 p-5 no-underline transition-colors hover:border-brand-500"
        >
          <div class="flex items-start justify-between gap-2">
            <h3 class="font-display text-base font-semibold text-slate-900">{{ u.name }}</h3>
            <span
              v-if="u.featured"
              class="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800"
            >Featured</span>
          </div>
          <p v-if="u.nameCn" class="mt-0.5 text-sm text-slate-500">{{ u.nameCn }}</p>
          <p class="mt-2 text-sm text-slate-600">{{ place(u) }}</p>
          <p v-if="typeLabel(u.type)" class="mt-1 text-xs uppercase tracking-wide text-slate-400">
            {{ typeLabel(u.type) }}
          </p>
        </NuxtLink>
      </div>
      <p v-else class="py-10 text-slate-600">No universities published yet.</p>
    </NContainer>
  </div>
</template>
