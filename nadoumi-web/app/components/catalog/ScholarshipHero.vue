<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'

defineProps<{ s: ScholarshipDetail, applyTo: string }>()

const { t } = useI18n()
const localePath = useLocalePath()

function fundingLabel(m?: string) {
  return m ? t(`scholarships.funding.${m}`) : ''
}
function langLabel(c?: string | null) {
  return c ? t(`scholarships.lang.${c}`) : ''
}
</script>

<template>
<section class="relative isolate overflow-hidden bg-slate-900 text-white">
  <img
    v-if="s.heroUrl ?? s.heroImageUrl ?? s.coverUrl ?? s.coverImageUrl"
    :src="mediaUrl(s.heroUrl ?? s.heroImageUrl ?? s.coverUrl ?? s.coverImageUrl)"
    alt=""
    class="absolute inset-0 -z-20 h-full w-full object-cover opacity-40"
  >
  <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-900 via-slate-900/90 to-brand-900/60" aria-hidden="true" />
  <NContainer>
    <div class="max-w-3xl py-12 sm:py-16">
      <div class="flex flex-wrap items-center gap-2 text-xs font-semibold">
        <span class="rounded-full bg-white/10 px-2.5 py-0.5 ring-1 ring-white/20">{{ fundingLabel(s.fundingModel) }}</span>
        <span v-if="s.hasStipend" class="rounded-full bg-white/10 px-2.5 py-0.5 ring-1 ring-white/20">{{ t('scholarships.withStipend') }}</span>
        <span v-if="s.hot" class="rounded-full bg-amber-400 px-2.5 py-0.5 text-amber-950">{{ t('scholarships.hot') }}</span>
        <span v-else-if="s.featured" class="rounded-full bg-brand-500 px-2.5 py-0.5">{{ t('catalog.featured') }}</span>
      </div>
      <p v-if="s.referenceCode" class="mt-3 font-mono text-xs uppercase tracking-widest text-white/60">{{ s.referenceCode }}</p>
      <h1 class="mt-1 font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ s.title }}</h1>
      <p v-if="s.summary" class="mt-3 text-lg text-slate-200">{{ s.summary }}</p>
      <dl class="mt-5 flex flex-wrap gap-x-8 gap-y-2 text-sm text-slate-300">
        <div><dt class="inline text-slate-400">{{ t('scholarships.colDeadline') }}: </dt><dd class="inline">{{ s.deadline ?? t('catalog.rollingDeadline') }}</dd></div>
        <div><dt class="inline text-slate-400">{{ t('scholarships.colLanguage') }}: </dt><dd class="inline">{{ langLabel(s.teachingLanguage) }}</dd></div>
        <div v-if="s.levels.length"><dt class="inline text-slate-400">{{ t('scholarships.colLevel') }}: </dt><dd class="inline">{{ s.levels.map(l => t(`scholarships.level.${l}`)).join(', ') }}</dd></div>
        <div v-if="s.nonDegreeDuration"><dt class="inline text-slate-400">{{ t('scholarships.nonDegreeDuration') }}: </dt><dd class="inline">{{ t(`scholarships.nonDegree.${s.nonDegreeDuration}`, s.nonDegreeDuration) }}</dd></div>
        <div v-if="s.studyDurationMonths"><dt class="inline text-slate-400">{{ t('scholarships.studyDuration') }}: </dt><dd class="inline">{{ t('scholarships.stipendDuration', { n: s.studyDurationMonths }) }}</dd></div>
        <div v-if="s.applicationChannel"><dt class="inline text-slate-400">{{ t('scholarships.applicationChannel') }}: </dt><dd class="inline">{{ t(`scholarships.channel.${s.applicationChannel}`, s.applicationChannel) }}<span v-if="s.agencyNumber"> ({{ s.agencyNumber }})</span></dd></div>
        <div v-if="[s.city, s.province, s.country].filter(Boolean).length"><dt class="inline text-slate-400">{{ t('scholarships.colLocation') }}: </dt><dd class="inline">{{ [s.city, s.province, s.country].filter(Boolean).join(', ') }}</dd></div>
      </dl>
      <div class="mt-7 flex flex-wrap gap-3">
        <NButton :to="applyTo" size="lg">{{ t('scholarships.applyNow') }}</NButton>
        <NButton :to="localePath('/scholarships')" variant="secondary" size="lg">{{ t('catalog.backToList') }}</NButton>
      </div>
      <p class="mt-2 text-xs text-slate-400">{{ t('scholarships.applyNote') }}</p>
    </div>
  </NContainer>
</section>
</template>
