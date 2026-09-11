<script setup lang="ts">
import type { GuideIconName } from '~/components/guides/icons'

const { t } = useI18n()
const localePath = useLocalePath()

const coverageKeys = ['fullyFunded', 'tuitionAccommodation', 'tuitionWaiver', 'partial'] as const
const coverageIcons: Record<(typeof coverageKeys)[number], GuideIconName> = {
  fullyFunded: 'award',
  tuitionAccommodation: 'home',
  tuitionWaiver: 'book',
  partial: 'wallet',
}
const coverage = coverageKeys.map(key => ({
  icon: coverageIcons[key],
  term: t(`guides.scholarshipTypes.coverage.${key}.term`),
  detail: t(`guides.scholarshipTypes.coverage.${key}.detail`),
}))
</script>

<template>
  <GuideLayout slug="scholarship-types">
    <p>{{ t('guides.scholarshipTypes.intro') }}</p>

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.csc.eyebrow')" :title="t('guides.scholarshipTypes.sections.csc.title')">
      <i18n-t keypath="guides.scholarshipTypes.csc.body" tag="p" scope="global">
        <template #coverage>
          <strong>{{ t('guides.scholarshipTypes.csc.coverage') }}</strong>
        </template>
        <template #stipend>
          {{ t('guides.scholarshipTypes.csc.stipend') }}
        </template>
        <template #insurance>
          <strong>{{ t('guides.scholarshipTypes.csc.insurance') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.provincial.eyebrow')" :title="t('guides.scholarshipTypes.sections.provincial.title')">
      <p>{{ t('guides.scholarshipTypes.provincial.body') }}</p>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.university.eyebrow')" :title="t('guides.scholarshipTypes.sections.university.title')">
      <i18n-t keypath="guides.scholarshipTypes.university.body" tag="p" scope="global">
        <template #renewable>
          <strong>{{ t('guides.scholarshipTypes.university.renewable') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.confucius.eyebrow')" :title="t('guides.scholarshipTypes.sections.confucius.title')">
      <i18n-t keypath="guides.scholarshipTypes.confucius.body" tag="p" scope="global">
        <template #scholarshipName>
          <strong>{{ t('guides.scholarshipTypes.confucius.scholarshipName') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.reference.eyebrow')" :title="t('guides.scholarshipTypes.sections.reference.title')">
      <GuideIconList :items="coverage" />
    </GuideSection>

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.improve.eyebrow')" :title="t('guides.scholarshipTypes.sections.improve.title')">
      <ul class="list-disc space-y-2 ps-6 marker:text-slate-400">
        <li>{{ t('guides.scholarshipTypes.improve.early') }}</li>
        <li>
          <i18n-t keypath="guides.scholarshipTypes.improve.language" tag="span" scope="global">
            <template #before>
              <strong>{{ t('guides.scholarshipTypes.improve.before') }}</strong>
            </template>
          </i18n-t>
        </li>
        <li>{{ t('guides.scholarshipTypes.improve.studyPlan') }}</li>
        <li>{{ t('guides.scholarshipTypes.improve.target') }}</li>
      </ul>
    </GuideSection>

    <PopularScholarships />

    <GuideSection :eyebrow="t('guides.scholarshipTypes.sections.next.eyebrow')" :title="t('guides.scholarshipTypes.sections.next.title')">
      <i18n-t keypath="guides.scholarshipTypes.next.body" tag="p" scope="global">
        <template #scholarships>
          <NuxtLink :to="localePath('/scholarships')">{{ t('guides.scholarshipTypes.next.scholarshipsLink') }}</NuxtLink>
        </template>
        <template #createProfile>
          <NuxtLink :to="localePath('/register')">{{ t('guides.scholarshipTypes.next.createProfileLink') }}</NuxtLink>
        </template>
      </i18n-t>
    </GuideSection>
  </GuideLayout>
</template>
