<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'
import type { DetailFact } from '~/components/catalog/DetailFacts.vue'

const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const { data: s } = await usePublicDetail<ScholarshipDetail>('scholarships')

useSeo(
  s.value?.title ?? t('catalog.scholarshipsTitle'),
  s.value?.summary?.slice(0, 155) ?? t('catalog.scholarshipsSubtitle'),
)

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))
const place = computed(() => [s.value?.city, s.value?.province, s.value?.country].filter(Boolean).join(', '))
const heroImage = computed(() => s.value?.coverUrl ?? s.value?.coverImageUrl ?? s.value?.heroUrl ?? s.value?.heroImageUrl)

const facts = computed<DetailFact[]>(() => {
  const x = s.value
  if (!x) return []
  const channel = x.applicationChannel
    ? t(`scholarships.channel.${x.applicationChannel}`, x.applicationChannel) + (x.agencyNumber ? ` (${x.agencyNumber})` : '')
    : ''
  const duration = x.nonDegreeDuration
    ? t(`scholarships.nonDegree.${x.nonDegreeDuration}`, x.nonDegreeDuration)
    : x.studyDurationMonths ? t('scholarships.stipendDuration', { n: x.studyDurationMonths }) : ''
  const all: (DetailFact | false)[] = [
    { label: t('scholarships.colDeadline'), value: x.deadline ?? t('catalog.rollingDeadline'), icon: 'calendar' },
    { label: t('scholarships.colLevel'), value: x.levels.map(l => t(`scholarships.level.${l}`)).join(', '), icon: 'award' },
    { label: t('scholarships.colLanguage'), value: x.teachingLanguage ? t(`scholarships.lang.${x.teachingLanguage}`) : '', icon: 'messages' },
    { label: t('scholarships.colField'), value: fieldsLabel(x.fields, t), icon: 'education' },
    { label: t('scholarships.studyDuration'), value: duration, icon: 'clock' },
    { label: t('scholarships.applicationChannel'), value: channel, icon: 'applications' },
  ]
  return all.filter((f): f is DetailFact => f !== false && f.value !== '')
})
</script>

<template>
  <DetailPage v-if="s" aside-first>
    <template #hero>
      <DetailHero
        :image="heroImage"
        :title="s.title"
        :eyebrow="s.referenceCode"
        :subtitle="place"
        :back-to="localePath('/scholarships')"
        :back-label="t('catalog.backToList')"
      >
        <template #badges>
          <HeroBadge>{{ t(`scholarships.funding.${s.fundingModel}`) }}</HeroBadge>
          <HeroBadge v-if="s.hasStipend">{{ t('scholarships.withStipend') }}</HeroBadge>
          <HeroBadge v-if="s.hot">{{ t('scholarships.hot') }}</HeroBadge>
          <HeroBadge v-if="s.featured">{{ t('catalog.featured') }}</HeroBadge>
          <HeroBadge v-if="s.recommended">{{ t('catalog.recommendedShort') }}</HeroBadge>
          <HeroBadge v-for="c in s.categories" :key="c">{{ t(`scholarships.category.${c}`, c) }}</HeroBadge>
        </template>
        <template #actions>
          <NButton :to="applyTo" size="lg">{{ t('scholarships.applyNow') }}</NButton>
        </template>
      </DetailHero>
    </template>

    <template #facts>
      <DetailFacts :items="facts" />
    </template>

    <DetailCard v-if="s.summary">
      <p class="leading-7 text-slate-700">{{ s.summary }}</p>
    </DetailCard>
    <ScholarshipSections :s="s" />

    <template #aside>
      <ScholarshipAside :s="s" />
    </template>
  </DetailPage>

  <DetailNotFound
    v-else
    :title="t('catalog.scholarshipsTitle')"
    :message="t('scholarships.notFound')"
    :back-to="localePath('/scholarships')"
    :back-label="t('catalog.backToList')"
  />
</template>
