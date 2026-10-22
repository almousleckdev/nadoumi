<script setup lang="ts">
import type { ProgramCard, UniversityDetail, UniversityHighlight } from '~/types/catalog'
import type { DetailFact } from '~/components/catalog/DetailFacts.vue'

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

const facts = computed<DetailFact[]>(() => {
  const x = u.value
  if (!x) return []
  const all: DetailFact[] = [
    { label: t('university.founded'), value: x.foundedYear ? String(x.foundedYear) : '', icon: 'calendar' },
    { label: t('university.students'), value: num(x.totalStudents), icon: 'contacts' },
    { label: t('university.intlStudents'), value: num(x.internationalStudents), icon: 'location' },
    { label: t('university.faculty'), value: num(x.facultyCount), icon: 'profile' },
    { label: t('university.rankingTier'), value: x.rankingTier ?? '', icon: 'award' },
  ]
  return all.filter(f => f.value)
})

const place = computed(() => [u.value?.city, u.value?.province, u.value?.country].filter(Boolean).join(', '))
const typeLabel = computed(() =>
  u.value?.type === 'PUBLIC' ? t('catalog.typePublic') : u.value?.type === 'PRIVATE' ? t('catalog.typePrivate') : '',
)

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
  <DetailPage v-if="u">
    <template #hero>
      <DetailHero
        :image="u.bannerUrl ?? u.coverImageUrl"
        :logo="u.logoUrl ?? u.logoImageUrl"
        :title="u.name"
        :eyebrow="u.nameCn"
        :subtitle="place"
        :back-to="localePath('/universities')"
        :back-label="t('catalog.allUniversities')"
      >
        <template #badges>
          <HeroBadge v-if="typeLabel">{{ typeLabel }}</HeroBadge>
          <HeroBadge v-if="u.recommended">{{ t('catalog.recommendedShort') }}</HeroBadge>
          <HeroBadge v-if="u.featured">{{ t('catalog.featured') }}</HeroBadge>
        </template>
      </DetailHero>
    </template>

    <template #facts>
      <DetailFacts :items="facts" />
    </template>

    <DetailCard v-for="p in prose" :key="p.heading" :title="p.heading">
      <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
    </DetailCard>

    <DetailCard v-if="highlightsOf('HIGHLIGHT').length" :title="t('university.highlights')">
      <BulletList :items="highlightsOf('HIGHLIGHT').map(h => ({ text: h.text }))" />
    </DetailCard>
    <DetailCard v-if="highlightsOf('ADVANTAGE').length" :title="t('university.advantages')">
      <BulletList :items="highlightsOf('ADVANTAGE').map(h => ({ text: h.text }))" />
    </DetailCard>

    <DetailCard :title="t('university.programmes')" :description="t('program.sectionIntro')">
      <div v-if="programs.length" class="grid gap-4 sm:grid-cols-2">
        <ProgramCard v-for="p in programs" :key="p.id" :program="p" :show-university="false" />
      </div>
      <p v-else class="rounded-xl border border-dashed border-slate-200 p-6 text-sm text-slate-500">
        {{ t('university.programmesEmpty') }}
      </p>
    </DetailCard>

    <UniversityGallery :images="u.gallery" />

    <template #aside>
      <UniversityAside :university="u" />
    </template>
  </DetailPage>

  <DetailNotFound
    v-else
    :title="t('catalog.universityFallbackTitle')"
    :message="t('catalog.universityUnavailable')"
    :back-to="localePath('/universities')"
    :back-label="t('catalog.allUniversities')"
  />
</template>
