<script setup lang="ts">
import type { GuideIconName } from '~/components/guides/icons'

const { t } = useI18n()
const localePath = useLocalePath()

function items(section: string, rows: { key: string, icon: GuideIconName }[]) {
  return rows.map(({ key, icon }) => ({
    icon,
    term: t(`guides.welcomeToChina.${section}.${key}.term`),
    detail: t(`guides.welcomeToChina.${section}.${key}.detail`),
  }))
}

const why = items('why', [
  { key: 'lowCost', icon: 'wallet' },
  { key: 'realScholarships', icon: 'award' },
  { key: 'wideChoice', icon: 'book' },
  { key: 'network', icon: 'globe' },
])
const life = items('life', [
  { key: 'phone', icon: 'qr' },
  { key: 'railway', icon: 'train' },
  { key: 'campus', icon: 'home' },
  { key: 'safety', icon: 'shield' },
])
</script>

<template>
  <GuideLayout slug="welcome-to-china">
    <p>{{ t('guides.welcomeToChina.intro') }}</p>

    <GuideSection :eyebrow="t('guides.welcomeToChina.sections.case.eyebrow')" :title="t('guides.welcomeToChina.sections.case.title')">
      <GuideIconList :items="why" />
    </GuideSection>

    <GuideSection :eyebrow="t('guides.welcomeToChina.sections.structure.eyebrow')" :title="t('guides.welcomeToChina.sections.structure.title')">
      <i18n-t keypath="guides.welcomeToChina.structureBody" tag="p" scope="global">
        <template #doubleFirstClass>
          <strong>{{ t('guides.welcomeToChina.doubleFirstClass') }}</strong>
        </template>
        <template #label985>
          <strong>{{ t('guides.welcomeToChina.label985') }}</strong>
        </template>
        <template #label211>
          <strong>{{ t('guides.welcomeToChina.label211') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.welcomeToChina.sections.language.eyebrow')" :title="t('guides.welcomeToChina.sections.language.title')">
      <i18n-t keypath="guides.welcomeToChina.languageBody" tag="p" scope="global">
        <template #hsk>
          <strong>{{ t('guides.welcomeToChina.hsk') }}</strong>
        </template>
        <template #ieltsToefl>
          <strong>{{ t('guides.welcomeToChina.ieltsToefl') }}</strong>
        </template>
      </i18n-t>
      <GuideCallout :title="t('guides.welcomeToChina.notReadyCallout.title')" icon="check" tone="brand">
        {{ t('guides.welcomeToChina.notReadyCallout.body') }}
      </GuideCallout>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.welcomeToChina.sections.dailyLife.eyebrow')" :title="t('guides.welcomeToChina.sections.dailyLife.title')">
      <GuideIconList :items="life" />
      <i18n-t keypath="guides.welcomeToChina.lifeNote" tag="p" scope="global">
        <template #livingGuide>
          <NuxtLink :to="localePath('/guides/living-in-china')">{{ t('guides.meta.living-in-china.title') }}</NuxtLink>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.welcomeToChina.sections.next.eyebrow')" :title="t('guides.welcomeToChina.sections.next.title')">
      <ol class="list-decimal space-y-2 ps-6 marker:text-slate-400">
        <li>
          <i18n-t keypath="guides.welcomeToChina.nextSteps.shortlist" tag="span" scope="global">
            <template #scholarships>
              <NuxtLink :to="localePath('/scholarships')">{{ t('nav.scholarships') }}</NuxtLink>
            </template>
            <template #universities>
              <NuxtLink :to="localePath('/universities')">{{ t('nav.universities') }}</NuxtLink>
            </template>
          </i18n-t>
        </li>
        <li>{{ t('guides.welcomeToChina.nextSteps.requirements') }}</li>
        <li>
          <i18n-t keypath="guides.welcomeToChina.nextSteps.documents" tag="span" scope="global">
            <template #howToApply>
              <NuxtLink :to="localePath('/guides/how-to-apply')">{{ t('guides.meta.how-to-apply.title') }}</NuxtLink>
            </template>
          </i18n-t>
        </li>
        <li>
          <i18n-t keypath="guides.welcomeToChina.nextSteps.visa" tag="span" scope="global">
            <template #jwForm>
              <strong>{{ t('guides.welcomeToChina.jwForm') }}</strong>
            </template>
            <template #visaGuide>
              <NuxtLink :to="localePath('/guides/student-visa-guide')">{{ t('guides.meta.student-visa-guide.title') }}</NuxtLink>
            </template>
          </i18n-t>
        </li>
      </ol>
      <i18n-t keypath="guides.welcomeToChina.finalCta" tag="p" scope="global">
        <template #createProfile>
          <NuxtLink :to="localePath('/register')">{{ t('guides.welcomeToChina.createProfileLink') }}</NuxtLink>
        </template>
      </i18n-t>
    </GuideSection>
  </GuideLayout>
</template>
