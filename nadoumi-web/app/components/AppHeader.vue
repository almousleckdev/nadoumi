<script setup lang="ts">
const { t } = useI18n()
const links = [
  { to: '/', key: 'nav.home' },
  { to: '/scholarships', key: 'nav.scholarships' },
  { to: '/universities', key: 'nav.universities' },
  { to: '/programs', key: 'nav.programs' },
  { to: '/about', key: 'nav.about' },
  { to: '/contact', key: 'nav.contact' },
]
</script>

<template>
  <header class="nad-header">
    <div class="nad-container nad-header__inner">
      <NuxtLink to="/" class="nad-header__brand">Nadoumi</NuxtLink>
      <nav class="nad-header__nav">
        <NuxtLink v-for="l in links" :key="l.to" :to="l.to">{{ t(l.key) }}</NuxtLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.nad-header { border-bottom: 1px solid var(--nad-border); }
.nad-header__inner { display: flex; align-items: center; justify-content: space-between; height: 4rem; }
.nad-header__brand { font-weight: 700; font-size: 1.15rem; color: var(--nad-fg); }
.nad-header__nav { display: flex; gap: 1.25rem; }
.nad-header__nav a { color: var(--nad-muted); }
</style>
