<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('destinations.title'), t('destinations.intro'))

const places = [
  { key: 'china', name: 'China' },
  { key: 'malaysia', name: 'Malaysia' },
]
</script>

<template>
  <div>
    <PageHero :title="t('destinations.title')" :subtitle="t('destinations.intro')" />
    <div class="grid gap-6 sm:grid-cols-2">
      <NCard v-for="p in places" :key="p.key">
        <h2 class="font-display text-lg font-semibold text-slate-900">{{ p.name }}</h2>
        <p class="mt-2 text-sm text-slate-600">{{ t(`destinations.${p.key}`) }}</p>
        <NuxtLink :to="localePath('/scholarships')" class="mt-3 inline-block text-sm text-brand-700 hover:underline">
          {{ t('destinations.browse') }}
        </NuxtLink>
      </NCard>
    </div>
    <p class="mt-6 text-sm text-slate-500">{{ t('destinations.more') }}</p>
  </div>
</template>
