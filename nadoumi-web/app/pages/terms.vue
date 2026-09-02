<script setup lang="ts">
const { t } = useI18n()
useSeo(t('legal.termsTitle'), t('legal.termsIntro'))
</script>

<template>
  <div>
    <PageHero :title="t('legal.termsTitle')" :subtitle="t('legal.draftNote')" />
    <div class="prose-sm max-w-none text-sm leading-6 text-slate-700">
      <p>{{ t('legal.termsIntro') }}</p>
      <p>{{ t('legal.termsBody') }}</p>
      <p class="text-slate-500">{{ t('legal.contactLine') }}</p>
    </div>
  </div>
</template>
