<script setup lang="ts">
import type { GuideIconName } from '~/components/guides/icons'

const { t } = useI18n()
const localePath = useLocalePath()

const documentKeys = ['admissionNotice', 'jwForm'] as const
const documentIcons: Record<(typeof documentKeys)[number], GuideIconName> = {
  admissionNotice: 'book',
  jwForm: 'passport',
}
const documents = documentKeys.map(key => ({
  icon: documentIcons[key],
  term: t(`guides.studentVisaGuide.documents.${key}.term`),
  detail: t(`guides.studentVisaGuide.documents.${key}.detail`),
}))

const arriveKeys = ['registerAddress', 'medicalCheck', 'residencePermit'] as const
const arrive = arriveKeys.map((key, i) => ({
  n: i + 1,
  key,
}))
</script>

<template>
  <GuideLayout slug="student-visa-guide">
    <i18n-t keypath="guides.studentVisaGuide.intro" tag="p" scope="global">
      <template #x1>
        <strong>{{ t('guides.studentVisaGuide.x1') }}</strong>
      </template>
      <template #residencePermit>
        <strong>{{ t('guides.studentVisaGuide.residencePermitTerm') }}</strong>
      </template>
      <template #x2>
        <strong>{{ t('guides.studentVisaGuide.x2') }}</strong>
      </template>
    </i18n-t>

    <GuideSection :eyebrow="t('guides.studentVisaGuide.sections.beforeApply.eyebrow')" :title="t('guides.studentVisaGuide.sections.beforeApply.title')">
      <GuideIconList :items="documents" />
      <p>{{ t('guides.studentVisaGuide.beforeApplyBody') }}</p>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.studentVisaGuide.sections.embassy.eyebrow')" :title="t('guides.studentVisaGuide.sections.embassy.title')">
      <ol class="list-decimal space-y-2 ps-6 marker:text-slate-400">
        <li>{{ t('guides.studentVisaGuide.embassySteps.cova') }}</li>
        <li>{{ t('guides.studentVisaGuide.embassySteps.gather') }}</li>
        <li>{{ t('guides.studentVisaGuide.embassySteps.appointment') }}</li>
        <li>{{ t('guides.studentVisaGuide.embassySteps.collect') }}</li>
      </ol>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.studentVisaGuide.sections.afterLand.eyebrow')" :title="t('guides.studentVisaGuide.sections.afterLand.title')">
      <ol class="space-y-4">
        <li v-for="s in arrive" :key="s.n" class="flex gap-4 rounded-xl border border-slate-200 bg-white p-4">
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{{ s.n }}</span>
          <span>
            <span class="block font-display font-semibold text-slate-900">{{ t(`guides.studentVisaGuide.arrive.${s.key}.title`) }}</span>
            <span class="mt-1 block text-sm leading-6 text-slate-600">{{ t(`guides.studentVisaGuide.arrive.${s.key}.body`) }}</span>
          </span>
        </li>
      </ol>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.studentVisaGuide.sections.whileStudying.eyebrow')" :title="t('guides.studentVisaGuide.sections.whileStudying.title')">
      <ul class="list-disc space-y-2 ps-6 marker:text-slate-400">
        <li>
          <i18n-t keypath="guides.studentVisaGuide.whileStudying.renew" tag="span" scope="global">
            <template #window>
              <strong>{{ t('guides.studentVisaGuide.whileStudying.renewWindow') }}</strong>
            </template>
          </i18n-t>
        </li>
        <li>{{ t('guides.studentVisaGuide.whileStudying.reregister') }}</li>
        <li>{{ t('guides.studentVisaGuide.whileStudying.workPermission') }}</li>
        <li>{{ t('guides.studentVisaGuide.whileStudying.changeUniversity') }}</li>
      </ul>
      <GuideCallout :title="t('guides.studentVisaGuide.beforeAllThis.title')" icon="check" tone="brand">
        <i18n-t keypath="guides.studentVisaGuide.beforeAllThis.body" tag="span" scope="global">
          <template #howToApply>
            <NuxtLink :to="localePath('/guides/how-to-apply')">{{ t('guides.meta.how-to-apply.title') }}</NuxtLink>
          </template>
        </i18n-t>
      </GuideCallout>
    </GuideSection>
  </GuideLayout>
</template>
