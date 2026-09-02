<script setup lang="ts">
import type { UniversityDetail, UniversityHighlight } from '~/types/catalog'

const route = useRoute()
const id = computed(() => String(route.params.id))

const { publicGet } = useApi()
const { data: u } = await useAsyncData(
  () => `university-${id.value}`,
  () => publicGet<UniversityDetail>(`universities/${id.value}`).catch(() => null),
  { watch: [id] },
)

useSeo(
  u.value?.name ?? 'University',
  u.value?.introduction?.slice(0, 155) ?? 'University profile and the programmes it offers.',
)

function place(x: UniversityDetail): string {
  return [x.city, x.province, x.country].filter(Boolean).join(', ')
}
function typeLabel(t: UniversityDetail['type']): string {
  return t === 'PUBLIC' ? 'Public' : t === 'PRIVATE' ? 'Private' : ''
}
function num(n?: number | null): string {
  return n == null ? '' : n.toLocaleString('en')
}
function highlightsOf(kind: UniversityHighlight['kind']): UniversityHighlight[] {
  return (u.value?.highlights ?? []).filter(h => h.kind === kind)
}

const facts = computed(() => {
  const x = u.value
  if (!x) return [] as { label: string, value: string }[]
  return [
    { label: 'Founded', value: x.foundedYear ? String(x.foundedYear) : '' },
    { label: 'Students', value: num(x.totalStudents) },
    { label: 'International students', value: num(x.internationalStudents) },
    { label: 'Faculty', value: num(x.facultyCount) },
    { label: 'Ranking tier', value: x.rankingTier ?? '' },
  ].filter(f => f.value)
})

const prose = computed(() => {
  const x = u.value
  if (!x) return [] as { heading: string, body: string }[]
  return [
    { heading: 'Introduction', body: x.introduction ?? '' },
    { heading: 'History', body: x.history ?? '' },
    { heading: 'Campus', body: x.campusInfo ?? '' },
    { heading: 'Accommodation', body: x.accommodationInfo ?? '' },
    { heading: 'Nearby', body: x.nearbyInfo ?? '' },
  ].filter(p => p.body)
})
</script>

<template>
  <div>
    <template v-if="u">
      <PageHero :title="u.name">
        <template #default>
          <p v-if="u.nameCn" class="text-slate-500">{{ u.nameCn }}</p>
          <p class="mt-1 text-slate-600">
            <span v-if="typeLabel(u.type)">{{ typeLabel(u.type) }} · </span>{{ place(u) }}
          </p>
        </template>
      </PageHero>

      <NContainer>
        <div class="space-y-10 py-6">
          <dl v-if="facts.length" class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            <div v-for="f in facts" :key="f.label" class="rounded-xl border border-slate-200 p-4">
              <dt class="text-xs uppercase tracking-wide text-slate-400">{{ f.label }}</dt>
              <dd class="mt-1 font-display text-lg font-semibold text-slate-900">{{ f.value }}</dd>
            </div>
          </dl>

          <section
            v-for="p in prose"
            :key="p.heading"
          >
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ p.heading }}</h2>
            <p class="mt-2 whitespace-pre-line text-slate-700">{{ p.body }}</p>
          </section>

          <section v-if="highlightsOf('HIGHLIGHT').length || highlightsOf('ADVANTAGE').length">
            <div class="grid gap-8 sm:grid-cols-2">
              <div v-if="highlightsOf('HIGHLIGHT').length">
                <h2 class="font-display text-xl font-semibold text-slate-900">Highlights</h2>
                <ul class="mt-2 list-disc space-y-1 ps-5 text-slate-700">
                  <li v-for="h in highlightsOf('HIGHLIGHT')" :key="h.id">{{ h.text }}</li>
                </ul>
              </div>
              <div v-if="highlightsOf('ADVANTAGE').length">
                <h2 class="font-display text-xl font-semibold text-slate-900">Advantages</h2>
                <ul class="mt-2 list-disc space-y-1 ps-5 text-slate-700">
                  <li v-for="h in highlightsOf('ADVANTAGE')" :key="h.id">{{ h.text }}</li>
                </ul>
              </div>
            </div>
          </section>

          <section v-if="u.rankings.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">Rankings</h2>
            <table class="mt-2 w-full max-w-md text-left text-sm">
              <thead class="text-slate-400">
                <tr>
                  <th class="py-1 font-medium">Source</th>
                  <th class="py-1 font-medium">Rank</th>
                  <th class="py-1 font-medium">Year</th>
                </tr>
              </thead>
              <tbody class="text-slate-700">
                <tr v-for="r in u.rankings" :key="r.id" class="border-t border-slate-100">
                  <td class="py-1.5">{{ r.source }}</td>
                  <td class="py-1.5">#{{ r.rankPosition }}</td>
                  <td class="py-1.5">{{ r.rankYear ?? '—' }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section>
            <h2 class="font-display text-xl font-semibold text-slate-900">Programmes</h2>
            <p class="mt-2 text-slate-600">
              This university's programmes — language, diploma, bachelor's, master's and PhD —
              will be listed here.
            </p>
          </section>

          <section v-if="u.website || u.admissionsEmail || u.officePhone" class="border-t border-slate-200 pt-6 text-sm text-slate-600">
            <p v-if="u.website">
              Website:
              <a :href="u.website" target="_blank" rel="noopener" class="text-brand-600 underline">{{ u.website }}</a>
            </p>
            <p v-if="u.admissionsEmail">Admissions: {{ u.admissionsEmail }}</p>
            <p v-if="u.officePhone">Phone: {{ u.officePhone }}</p>
          </section>

          <NuxtLink to="/universities" class="inline-block text-sm text-brand-600 underline">← All universities</NuxtLink>
        </div>
      </NContainer>
    </template>

    <template v-else>
      <PageHero title="University" />
      <NContainer>
        <p class="py-10 text-slate-600">This university is not available.</p>
        <NuxtLink to="/universities" class="text-sm text-brand-600 underline">← All universities</NuxtLink>
      </NContainer>
    </template>
  </div>
</template>
