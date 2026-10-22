<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'
import type { DetailFact } from '~/components/catalog/DetailFacts.vue'
import { programTuition } from '~/utils/programDisplay'

const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const { data: p } = await usePublicDetail<ProgramDetail>('programs')

useSeo(
  p.value?.name ?? t('program.fallbackTitle'),
  p.value?.summary?.slice(0, 155) ?? t('home.programDiscovery.description'),
)

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))
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
  const all: DetailFact[] = [
    { label: t('program.language'), value: x.teachingLanguage ? t(`program.lang.${x.teachingLanguage}`) : '', icon: 'messages' },
    { label: t('program.field'), value: x.field ?? '', icon: 'education' },
    { label: t('program.durationLabel'), value: x.durationMonths ? t('program.duration', { months: x.durationMonths }) : '', icon: 'clock' },
    { label: t('program.tuition'), value: programTuition(x).cny, icon: 'award', tone: 'price' },
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
