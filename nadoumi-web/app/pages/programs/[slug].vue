<script setup lang="ts">
import { applyRoute } from '~/utils/applyView'
import type { ProgramDetail } from '~/types/catalog'
import type { DetailFact } from '~/components/catalog/DetailFacts.vue'
import { cheapest, durationRange, formatDuration, offeringMoney } from '~/utils/programDisplay'

const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const { data: p } = await usePublicDetail<ProgramDetail>('programs')
const { publicGet } = useApi()
// the same rows that drive the student's checklist, so visitors read what students are asked for
const { data: requirements } = await useAsyncData(
  () => `program-documents-${p.value?.id ?? 0}`,
  async () => p.value
    ? (await publicGet<{ docType: string, mandatory: boolean, notes?: string | null, appliesWhen?: string | null }[]>(
        `document-requirements/programs/${p.value.id}`).catch(() => []))
        .map(({ notes, ...rest }) => ({ ...rest, note: notes ?? null }))
    : [],
  { default: () => [] },
)

useSeo(
  p.value?.name ?? t('program.fallbackTitle'),
  p.value?.summary?.slice(0, 155) ?? t('home.programDiscovery.description'),
)

// a signed-in student goes straight to the apply screen; a guest signs in first and is returned to it
const applyTo = computed(() => {
  const slug = p.value?.slug
  if (!slug) return localePath('/register')
  const apply = localePath(applyRoute('programme', slug))
  return status.value === 'authed' ? apply : localePath({ path: '/login', query: { redirect: apply } })
})
const universityTo = computed(() =>
  p.value ? localePath(`/universities/${p.value.universitySlug ?? p.value.universityId}`) : '#')

const levelLabels = computed(() => {
  const x = p.value
  if (!x) return []
  return x.programType === 'DEGREE' && x.levels?.length
    ? x.levels.map(l => t(`program.level.${l}`))
    : [t(`program.level.${x.programType}`)]
})

const facts = computed<DetailFact[]>(() => {
  const x = p.value
  if (!x) return []
  const range = durationRange(x.offerings ?? [])
  const duration = !range ? '' : range.min === range.max
    ? formatDuration(range.min, t)
    : `${formatDuration(range.min, t)} – ${formatDuration(range.max, t)}`
  const tuition = cheapest(x.offerings ?? [], 'tuitionAmount')
  const room = cheapest(x.offerings ?? [], 'accommodationAmount')
  const all: DetailFact[] = [
    { label: t('program.language'), value: x.teachingLanguage ? t(`program.lang.${x.teachingLanguage}`) : '', icon: 'messages' },
    { label: t('program.field'), value: x.field ?? '', icon: 'education' },
    { label: t('program.durationLabel'), value: duration, icon: 'clock' },
    { label: t('program.tuitionFrom'), value: offeringMoney(tuition?.tuitionAmount, tuition?.tuitionAmountUsd, tuition?.tuitionCurrency), icon: 'award', tone: 'price' },
    { label: t('program.accommodationFrom'), value: offeringMoney(room?.accommodationAmount, room?.accommodationAmountUsd, room?.accommodationCurrency), icon: 'location', tone: 'price' },
  ]
  return all.filter(f => f.value)
})
</script>

<template>
  <DetailPage v-if="p" aside-first>
    <template #hero>
      <DetailHero
        :image="p.imageUrl"
        :title="p.name"
        :eyebrow="p.nameCn"
        :subtitle="p.universityName ? t('program.offeredBy', { university: p.universityName }) : null"
        :back-to="localePath('/programs')"
        :back-label="t('program.viewAll')"
      >
        <template #badges>
          <HeroBadge v-for="lv in levelLabels" :key="lv">{{ lv }}</HeroBadge>
          <HeroBadge v-if="p.hot">{{ t('program.hot') }}</HeroBadge>
          <HeroBadge v-if="p.featured">{{ t('catalog.featured') }}</HeroBadge>
        </template>
        <template #actions>
          <NButton :to="applyTo" size="lg">{{ t('program.applyNow') }}</NButton>
        </template>
      </DetailHero>
    </template>

    <template #facts>
      <DetailFacts :items="facts" />
    </template>

    <DetailCard v-if="p.summary" :title="t('program.fallbackTitle')">
      <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.summary }}</p>
    </DetailCard>
    <ProgramOfferings :offerings="p.offerings ?? []" />
    <ProgramAdmission :program="p" :documents="requirements" />
    <ProgramMajors :majors="p.majors ?? []" />
    <ProgramIntakes :intakes="p.intakes" />

    <template #aside>
      <ProgramAside :program="p" :apply-to="applyTo" :university-to="universityTo" />
    </template>
  </DetailPage>

  <DetailNotFound
    v-else
    :title="t('program.fallbackTitle')"
    :message="t('program.notAvailable')"
    :back-to="localePath('/programs')"
    :back-label="t('program.viewAll')"
  />
</template>
