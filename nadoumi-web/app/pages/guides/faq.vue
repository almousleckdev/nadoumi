<script setup lang="ts">
import type { MessageFunction, VueMessageType } from 'vue-i18n'
import type { GuideIconName } from '~/components/guides/icons'

const { t, tm, rt } = useI18n()
const localePath = useLocalePath()

interface FaqGroupMeta {
  key: string
  icon: GuideIconName
}

const groupMeta: FaqGroupMeta[] = [
  { key: 'studyingAndLanguage', icon: 'book' },
  { key: 'costAndFunding', icon: 'wallet' },
  { key: 'applying', icon: 'calendar' },
  { key: 'visaAndFamily', icon: 'passport' },
  { key: 'afterGraduation', icon: 'award' },
]

type RawMessage = VueMessageType | MessageFunction<VueMessageType>

// `tm` reads the raw array of {q, a} pairs so each locale can hold its own
// Q&A list without a fixed positional shape. vue-i18n may have already
// compiled some of those strings into message functions (it compiles a
// message in place the first time it is resolved), so every value must go
// through `rt` before it is safe to render, per the vue-i18n `tm` docs.
function faqItems(groupKey: string): { q: string, a: string }[] {
  const raw = tm(`guides.faq.groups.${groupKey}.items`) as { q: RawMessage, a: RawMessage }[]
  return raw.map(item => ({ q: rt(item.q), a: rt(item.a) }))
}
</script>

<template>
  <GuideLayout slug="faq">
    <p>{{ t('guides.faq.intro') }}</p>

    <GuideSection v-for="group in groupMeta" :key="group.key" :title="t(`guides.faq.groups.${group.key}.title`)" :icon="group.icon">
      <div class="space-y-3">
        <details
          v-for="(f, i) in faqItems(group.key)"
          :key="i"
          class="group rounded-xl border border-slate-200 bg-white p-4 open:shadow-sm"
        >
          <summary class="flex cursor-pointer list-none items-start gap-3 font-semibold text-slate-900 marker:content-none">
            <span class="mt-0.5 shrink-0 text-lg leading-none text-brand-600 group-open:hidden">+</span>
            <span class="mt-0.5 hidden shrink-0 text-lg leading-none text-brand-600 group-open:inline">&minus;</span>
            <span>{{ f.q }}</span>
          </summary>
          <p class="mt-3 ps-6 leading-7 text-slate-600">{{ f.a }}</p>
        </details>
      </div>
    </GuideSection>

    <GuideSection :eyebrow="t('guides.faq.sections.next.eyebrow')" :title="t('guides.faq.sections.next.title')">
      <i18n-t keypath="guides.faq.stillUnsure" tag="p" scope="global">
        <template #contact>
          <NuxtLink :to="localePath('/contact')">{{ t('nav.contact') }}</NuxtLink>
        </template>
        <template #createProfile>
          <NuxtLink :to="localePath('/register')">{{ t('guides.faq.createProfileLink') }}</NuxtLink>
        </template>
      </i18n-t>
    </GuideSection>
  </GuideLayout>
</template>
