<script setup lang="ts">
const { t } = useI18n()
useSeo(t('legal.privacyTitle'), t('legal.privacyIntro'))
</script>

<template>
  <div>
    <PageHero :title="t('legal.privacyTitle')" :subtitle="t('legal.draftNote')" />
    <div class="prose-sm max-w-none text-sm leading-6 text-slate-700">
      <p>{{ t('legal.privacyIntro') }}</p>
      <p>{{ t('legal.privacyBody') }}</p>
      <p class="text-slate-500">{{ t('legal.contactLine') }}</p>
    </div>
  </div>
</template>
