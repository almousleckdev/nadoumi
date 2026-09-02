<script setup lang="ts">
const { t } = useI18n()
useSeo(t('home.title'), t('home.subtitle'))

const sections = [
  { to: '/scholarships', title: 'Scholarships', body: 'Funding opportunities by country, field and degree.' },
  { to: '/universities', title: 'Universities', body: 'Institutions worldwide — with the programmes each one offers.' },
]
</script>

<template>
  <div>
    <PageHero :title="t('home.title')" :subtitle="t('home.subtitle')" />
    <div class="nad-grid">
      <ContentCard v-for="s in sections" :key="s.to" :title="s.title" :to="s.to">
        {{ s.body }}
      </ContentCard>
    </div>
  </div>
</template>
