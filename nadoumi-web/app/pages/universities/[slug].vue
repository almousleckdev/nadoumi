<script setup lang="ts">
import type { ProgramCard, UniversityDetail, UniversityHighlight } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet } = useApi()
const { slug, data: u } = await usePublicDetail<UniversityDetail>('universities')
const { data: programs } = await useAsyncData(
  () => `university-${slug.value}-programs`,
  () => publicGet<ProgramCard[]>(`universities/${slug.value}/programs`).catch(() => []),
  { watch: [slug], default: () => [] },
)

useSeo(
  u.value?.name ?? t('catalog.universityFallbackTitle'),
  u.value?.introduction?.slice(0, 155) ?? t('catalog.universitiesSubtitle'),
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
    <UniversityHero :university="u" />

    <NContainer>
      <FactTiles v-if="facts.length" :items="facts" class="relative z-10 mt-6 lg:-mt-6" />
      <div class="grid gap-8 py-8 lg:grid-cols-[minmax(0,1fr)_20rem] lg:gap-10 lg:py-10">
        <div class="min-w-0 space-y-6">
          <DetailCard v-for="p in prose" :key="p.heading" :title="p.heading">
            <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
          </DetailCard>

          <div v-if="highlightsOf('HIGHLIGHT').length || highlightsOf('ADVANTAGE').length" class="grid gap-6 md:grid-cols-2">
            <DetailCard v-if="highlightsOf('HIGHLIGHT').length" :title="t('university.highlights')">
              <ul class="space-y-2.5">
                <li v-for="h in highlightsOf('HIGHLIGHT')" :key="h.id" class="flex gap-2.5 text-slate-700">
                  <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />{{ h.text }}
                </li>
              </ul>
            </DetailCard>
            <DetailCard v-if="highlightsOf('ADVANTAGE').length" :title="t('university.advantages')">
              <ul class="space-y-2.5">
                <li v-for="h in highlightsOf('ADVANTAGE')" :key="h.id" class="flex gap-2.5 text-slate-700">
                  <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />{{ h.text }}
                </li>
              </ul>
            </DetailCard>
          </div>

          <DetailCard :title="t('university.programmes')" :description="t('program.sectionIntro')">
            <div v-if="programs.length" class="grid gap-4 sm:grid-cols-2">
              <ProgramCard
                v-for="p in programs"
                :key="p.id"
                :program="p"
                :show-university="false"
              />
            </div>
            <p v-else class="rounded-xl border border-dashed border-slate-200 bg-slate-50/60 p-6 text-sm text-slate-500">
              {{ t('university.programmesEmpty') }}
            </p>
          </DetailCard>

          <UniversityGallery :images="u.gallery" />
        </div>

        <UniversitySidebar :university="u" />
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
