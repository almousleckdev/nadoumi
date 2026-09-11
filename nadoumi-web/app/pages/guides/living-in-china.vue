<script setup lang="ts">
import type { GuideIconName } from '~/components/guides/icons'

const { t } = useI18n()
const localePath = useLocalePath()

function items(section: string, rows: { key: string, icon: GuideIconName }[]) {
  return rows.map(({ key, icon }) => ({
    icon,
    term: t(`guides.livingInChina.${section}.${key}.term`),
    detail: t(`guides.livingInChina.${section}.${key}.detail`),
  }))
}

const pay = items('pay', [
  { key: 'walletApps', icon: 'qr' },
  { key: 'bankAccount', icon: 'wallet' },
  { key: 'budget', icon: 'bag' },
])
const move = items('move', [
  { key: 'metro', icon: 'metro' },
  { key: 'railway', icon: 'train' },
  { key: 'didi', icon: 'phone' },
  { key: 'bikes', icon: 'bike' },
])
const daily = items('daily', [
  { key: 'canteens', icon: 'food' },
  { key: 'delivery', icon: 'phone' },
  { key: 'shopping', icon: 'bag' },
  { key: 'sim', icon: 'wifi' },
])
</script>

<template>
  <GuideLayout slug="living-in-china">
    <p>{{ t('guides.livingInChina.intro') }}</p>

    <GuideSection :eyebrow="t('guides.livingInChina.sections.money.eyebrow')" :title="t('guides.livingInChina.sections.money.title')">
      <GuideIconList :items="pay" />
      <p>{{ t('guides.livingInChina.moneyNote') }}</p>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.livingInChina.sections.transport.eyebrow')" :title="t('guides.livingInChina.sections.transport.title')">
      <GuideIconList :items="move" />
      <GuideCallout :title="t('guides.livingInChina.documentsCallout.title')" icon="passport" tone="amber">
        {{ t('guides.livingInChina.documentsCallout.body') }}
      </GuideCallout>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.livingInChina.sections.home.eyebrow')" :title="t('guides.livingInChina.sections.home.title')">
      <i18n-t keypath="guides.livingInChina.homeBody" tag="p" scope="global">
        <template #dorms>
          <strong>{{ t('guides.livingInChina.onCampusDorms') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.livingInChina.sections.everyday.eyebrow')" :title="t('guides.livingInChina.sections.everyday.title')">
      <GuideIconList :items="daily" />
      <i18n-t keypath="guides.livingInChina.everydayNote" tag="p" scope="global">
        <template #wechat>
          <strong>{{ t('guides.livingInChina.wechat') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.livingInChina.sections.health.eyebrow')" :title="t('guides.livingInChina.sections.health.title')">
      <i18n-t keypath="guides.livingInChina.healthBody" tag="p" scope="global">
        <template #insurance>
          <strong>{{ t('guides.livingInChina.insuranceTerm') }}</strong>
        </template>
      </i18n-t>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.livingInChina.sections.settlingIn.eyebrow')" :title="t('guides.livingInChina.sections.settlingIn.title')">
      <ul class="list-disc space-y-2 ps-6 marker:text-slate-400">
        <li>{{ t('guides.livingInChina.settlingIn.mandarin') }}</li>
        <li>{{ t('guides.livingInChina.settlingIn.orientation') }}</li>
        <li>{{ t('guides.livingInChina.settlingIn.copies') }}</li>
        <li>
          <i18n-t keypath="guides.livingInChina.settlingIn.registration" tag="span" scope="global">
            <template #visaGuide>
              <NuxtLink :to="localePath('/guides/student-visa-guide')">{{ t('guides.meta.student-visa-guide.title') }}</NuxtLink>
            </template>
          </i18n-t>
        </li>
      </ul>
    </GuideSection>
  </GuideLayout>
</template>
