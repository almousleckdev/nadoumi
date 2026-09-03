<script setup lang="ts">
import type { ProgramCard, UniversityDetail, UniversityHighlight } from '~/types/catalog'

const route = useRoute()
const { t } = useI18n()
const localePath = useLocalePath()
const id = computed(() => String(route.params.id))

const { publicGet } = useApi()
const { data: u } = await useAsyncData(
  () => `university-${id.value}`,
  () => publicGet<UniversityDetail>(`universities/${id.value}`).catch(() => null),
  { watch: [id] },
)
const { data: programs } = await useAsyncData(
  () => `university-${id.value}-programs`,
  () => publicGet<ProgramCard[]>(`universities/${id.value}/programs`).catch(() => []),
  { watch: [id], default: () => [] },
)

useSeo(
  u.value?.name ?? t('catalog.universityFallbackTitle'),
  u.value?.introduction?.slice(0, 155) ?? t('catalog.universitiesSubtitle'),
)

const monogram = computed(() => u.value?.name.trim().charAt(0).toUpperCase() ?? 'N')
const place = computed(() => {
  const x = u.value
  return x ? [x.city, x.province, x.country].filter(Boolean).join(', ') : ''
})
const typeLabel = computed(() =>
  u.value?.type === 'PUBLIC' ? t('catalog.typePublic') : u.value?.type === 'PRIVATE' ? t('catalog.typePrivate') : '',
)
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
    { label: t('university.founded'), value: x.foundedYear ? String(x.foundedYear) : '' },
    { label: t('university.students'), value: num(x.totalStudents) },
    { label: t('university.intlStudents'), value: num(x.internationalStudents) },
    { label: t('university.faculty'), value: num(x.facultyCount) },
    { label: t('university.rankingTier'), value: x.rankingTier ?? '' },
  ].filter(f => f.value)
})

const prose = computed(() => {
  const x = u.value
  if (!x) return [] as { heading: string, body: string }[]
  return [
    { heading: t('university.introduction'), body: x.introduction ?? '' },
    { heading: t('university.history'), body: x.history ?? '' },
    { heading: t('university.campus'), body: x.campusInfo ?? '' },
    { heading: t('university.accommodation'), body: x.accommodationInfo ?? '' },
    { heading: t('university.nearby'), body: x.nearbyInfo ?? '' },
  ].filter(p => p.body)
})
</script>

<template>
  <div v-if="u">
    <!-- hero header -->
    <section class="relative isolate overflow-hidden bg-slate-900 text-white">
      <img
        v-if="u.coverImageUrl"
        :src="u.coverImageUrl"
        alt=""
        class="absolute inset-0 -z-20 h-full w-full object-cover opacity-40"
      >
      <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-900 via-slate-900/90 to-brand-900/60" aria-hidden="true" />
      <NContainer>
        <div class="flex flex-col gap-5 py-12 sm:flex-row sm:items-center sm:py-16">
          <img
            v-if="u.logoImageUrl"
            :src="u.logoImageUrl"
            alt=""
            class="h-20 w-20 shrink-0 rounded-2xl bg-white/95 object-contain p-2 ring-1 ring-white/20"
          >
          <span v-else class="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-white/10 font-display text-3xl font-bold ring-1 ring-white/20">
            {{ monogram }}
          </span>
          <div>
            <h1 class="font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ u.name }}</h1>
            <p v-if="u.nameCn" class="mt-1 text-lg text-white/70">{{ u.nameCn }}</p>
            <p class="mt-2 text-white/80">{{ place }}</p>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <span v-if="typeLabel" class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium ring-1 ring-white/20">{{ typeLabel }}</span>
              <span v-if="u.recommended" class="rounded-full bg-brand-500/90 px-2.5 py-0.5 text-xs font-semibold">{{ t('catalog.recommendedShort') }}</span>
              <span v-if="u.featured" class="rounded-full bg-white/90 px-2.5 py-0.5 text-xs font-semibold text-brand-800">{{ t('catalog.featured') }}</span>
            </div>
          </div>
        </div>
      </NContainer>
    </section>

    <NContainer>
      <div class="grid gap-10 py-10 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <div class="min-w-0 space-y-12">
          <dl v-if="facts.length" class="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <div v-for="f in facts" :key="f.label" class="rounded-xl border border-slate-200 bg-white p-4">
              <dt class="text-xs uppercase tracking-wide text-slate-400">{{ f.label }}</dt>
              <dd class="mt-1 font-display text-lg font-semibold text-slate-900">{{ f.value }}</dd>
            </div>
          </dl>

          <section v-for="p in prose" :key="p.heading">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ p.heading }}</h2>
            <p class="mt-2 whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
          </section>

          <section v-if="highlightsOf('HIGHLIGHT').length || highlightsOf('ADVANTAGE').length">
            <div class="grid gap-8 sm:grid-cols-2">
              <div v-if="highlightsOf('HIGHLIGHT').length">
                <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('university.highlights') }}</h2>
                <ul class="mt-3 space-y-2">
                  <li v-for="h in highlightsOf('HIGHLIGHT')" :key="h.id" class="flex gap-2 text-slate-700">
                    <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />{{ h.text }}
                  </li>
                </ul>
              </div>
              <div v-if="highlightsOf('ADVANTAGE').length">
                <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('university.advantages') }}</h2>
                <ul class="mt-3 space-y-2">
                  <li v-for="h in highlightsOf('ADVANTAGE')" :key="h.id" class="flex gap-2 text-slate-700">
                    <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />{{ h.text }}
                  </li>
                </ul>
              </div>
            </div>
          </section>

          <section>
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('university.programmes') }}</h2>
            <p class="mt-1 text-sm text-slate-600">{{ t('program.sectionIntro') }}</p>
            <div v-if="programs.length" class="mt-4 grid gap-4 sm:grid-cols-2">
              <ProgramCard
                v-for="p in programs"
                :key="p.id"
                :program="p"
                :show-university="false"
              />
            </div>
            <p v-else class="mt-3 rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 p-6 text-sm text-slate-500">
              {{ t('university.programmesEmpty') }}
            </p>
          </section>

          <section v-if="u.gallery.length">
            <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('university.gallery') }}</h2>
            <div class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
              <figure v-for="g in u.gallery" :key="g.id" class="overflow-hidden rounded-xl bg-slate-100">
                <img
                  :src="g.imageUrl"
                  :alt="g.caption ?? ''"
                  loading="lazy"
                  decoding="async"
                  class="aspect-[4/3] w-full object-cover"
                >
                <figcaption v-if="g.caption" class="px-2 py-1.5 text-xs text-slate-500">{{ g.caption }}</figcaption>
              </figure>
            </div>
          </section>
        </div>

        <aside class="space-y-6 lg:sticky lg:top-24 lg:self-start">
          <div v-if="u.rankings.length" class="rounded-xl border border-slate-200 bg-white p-5">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('university.rankings') }}</h2>
            <ul class="mt-3 space-y-2 text-sm">
              <li v-for="r in u.rankings" :key="r.id" class="flex items-baseline justify-between gap-3">
                <span class="text-slate-700">{{ r.source }}<span v-if="r.rankYear" class="text-slate-400"> · {{ r.rankYear }}</span></span>
                <span class="font-display font-semibold text-slate-900">#{{ r.rankPosition }}</span>
              </li>
            </ul>
          </div>

          <div v-if="u.website || u.admissionsEmail || u.officePhone" class="rounded-xl border border-slate-200 bg-white p-5 text-sm">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('university.contact') }}</h2>
            <ul class="mt-3 space-y-2 text-slate-700">
              <li v-if="u.website">
                <a :href="u.website" target="_blank" rel="noopener" class="text-brand-700 hover:text-brand-800">{{ t('university.website') }} ↗</a>
              </li>
              <li v-if="u.admissionsEmail">{{ t('university.admissionsEmail') }}: {{ u.admissionsEmail }}</li>
              <li v-if="u.officePhone">{{ t('university.officePhone') }}: {{ u.officePhone }}</li>
            </ul>
          </div>

          <NuxtLink :to="localePath('/universities')" class="inline-flex items-center gap-1.5 text-sm font-medium text-brand-700 hover:text-brand-800">
            <span aria-hidden="true">←</span> {{ t('catalog.allUniversities') }}
          </NuxtLink>
        </aside>
      </div>
    </NContainer>
  </div>

  <div v-else>
    <PageHero :title="t('catalog.universityFallbackTitle')" />
    <NContainer>
      <p class="py-10 text-slate-600">{{ t('catalog.universityUnavailable') }}</p>
      <NuxtLink :to="localePath('/universities')" class="text-sm font-medium text-brand-700 hover:text-brand-800">
        ← {{ t('catalog.allUniversities') }}
      </NuxtLink>
    </NContainer>
  </div>
</template>
