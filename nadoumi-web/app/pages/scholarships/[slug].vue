<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const { data: s } = await usePublicDetail<ScholarshipDetail>('scholarships')

useSeo(
  s.value?.title ?? t('catalog.scholarshipsTitle'),
  s.value?.summary?.slice(0, 155) ?? t('catalog.scholarshipsSubtitle'),
)

const facts = computed(() => {
  const x = s.value
  if (!x) return []
  const channel = x.applicationChannel
    ? t(`scholarships.channel.${x.applicationChannel}`, x.applicationChannel) + (x.agencyNumber ? ` (${x.agencyNumber})` : '')
    : ''
  return [
    { label: t('scholarships.colDeadline'), value: x.deadline ?? t('catalog.rollingDeadline') },
    { label: t('scholarships.colLevel'), value: x.levels.map(l => t(`scholarships.level.${l}`)).join(', ') },
    { label: t('scholarships.colLanguage'), value: x.teachingLanguage ? t(`scholarships.lang.${x.teachingLanguage}`) : '' },
    { label: t('scholarships.colField'), value: fieldsLabel(x.fields, t) },
    { label: t('scholarships.nonDegreeDuration'), value: x.nonDegreeDuration ? t(`scholarships.nonDegree.${x.nonDegreeDuration}`, x.nonDegreeDuration) : '' },
    { label: t('scholarships.studyDuration'), value: x.studyDurationMonths ? t('scholarships.stipendDuration', { n: x.studyDurationMonths }) : '' },
    { label: t('scholarships.applicationChannel'), value: channel },
  ].filter(f => f.value)
})

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))
</script>

<template>
  <div v-if="s">
    <ScholarshipHero :s="s" :apply-to="applyTo" />

    <NContainer>
      <FactTiles v-if="facts.length" :items="facts" class="relative z-10 mt-6 lg:-mt-6" />
      <div class="grid gap-8 py-8 lg:grid-cols-[minmax(0,1fr)_21rem] lg:gap-10 lg:py-10">
        <ScholarshipSections :s="s" />
        <ScholarshipSidebar :s="s" class="order-first lg:order-none" />
      </div>
    </NContainer>
  </div>

  <div v-else>
    <PageHero :title="t('catalog.scholarshipsTitle')" />
    <NContainer>
      <p class="py-10 text-slate-600">{{ t('scholarships.notFound') }}</p>
      <NuxtLink :to="localePath('/scholarships')" class="text-sm font-medium text-brand-700">← {{ t('catalog.backToList') }}</NuxtLink>
    </NContainer>
  </div>
</template>
