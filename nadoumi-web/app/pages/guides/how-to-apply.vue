<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()

const steps = [
  { n: 1, key: 'choosePrograms' },
  { n: 2, key: 'prepareDocuments' },
  { n: 3, key: 'submitApplication' },
  { n: 4, key: 'interviews' },
  { n: 5, key: 'acceptVisa' },
  { n: 6, key: 'beforeYouFly' },
] as const

const docs = [
  { icon: 'passport', key: 'passport' },
  { icon: 'book', key: 'diploma' },
  { icon: 'globe', key: 'language' },
  { icon: 'check', key: 'studyPlan' },
  { icon: 'chat', key: 'recommendations' },
  { icon: 'hospital', key: 'physicalExam' },
] as const
</script>

<template>
  <GuideLayout slug="how-to-apply">
    <i18n-t keypath="guides.howToApply.intro" tag="p" scope="global">
      <template #openWindow>
        <strong>{{ t('guides.howToApply.openWindow') }}</strong>
      </template>
      <template #deadlineWindow>
        <strong>{{ t('guides.howToApply.deadlineWindow') }}</strong>
      </template>
    </i18n-t>

    <GuideSection :eyebrow="t('guides.howToApply.sections.journey.eyebrow')" :title="t('guides.howToApply.sections.journey.title')">
      <ol class="space-y-4">
        <li v-for="s in steps" :key="s.n" class="flex gap-4 rounded-xl border border-slate-200 bg-white p-4">
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{{ s.n }}</span>
          <span>
            <span class="block font-display font-semibold text-slate-900">{{ t(`guides.howToApply.steps.${s.key}.title`) }}</span>
            <span class="mt-1 block text-sm leading-6 text-slate-600">{{ t(`guides.howToApply.steps.${s.key}.body`) }}</span>
          </span>
        </li>
      </ol>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.howToApply.sections.checklist.eyebrow')" :title="t('guides.howToApply.sections.checklist.title')">
      <GuideIconList :items="docs.map(d => ({ icon: d.icon, term: t(`guides.howToApply.docs.${d.key}.term`), detail: t(`guides.howToApply.docs.${d.key}.detail`) }))" />
      <GuideCallout :title="t('guides.howToApply.under18.title')" icon="shield" tone="amber">
        {{ t('guides.howToApply.under18.body') }}
      </GuideCallout>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.howToApply.sections.support.eyebrow')" :title="t('guides.howToApply.sections.support.title')">
      <i18n-t keypath="guides.howToApply.supportBody" tag="p" scope="global">
        <template #createProfile>
          <NuxtLink :to="localePath('/register')">{{ t('guides.howToApply.createProfile') }}</NuxtLink>
        </template>
        <template #visaGuide>
          <NuxtLink :to="localePath('/guides/student-visa-guide')">{{ t('guides.meta.student-visa-guide.title') }}</NuxtLink>
        </template>
      </i18n-t>
    </GuideSection>
  </GuideLayout>
</template>
