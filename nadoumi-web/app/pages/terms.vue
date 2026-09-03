<script setup lang="ts">
const { t } = useI18n()
useSeo(t('legal.termsTitle'), t('legal.termsIntro'))
</script>

<template>
  <div>
    <PageHero :title="t('legal.termsTitle')" :subtitle="t('legal.draftNote')" />
    <NContainer>
      <div class="max-w-2xl space-y-3 py-10 text-sm leading-6 text-slate-700">
        <p>{{ t('legal.termsIntro') }}</p>
        <p>{{ t('legal.termsBody') }}</p>
        <p class="text-slate-500">{{ t('legal.contactLine') }}</p>
      </div>
    </NContainer>
  </div>
</template>
