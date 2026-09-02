<script setup lang="ts">
defineProps<{ title: string; meta?: string; to?: string }>()
</script>

<template>
  <component :is="to ? 'NuxtLink' : 'div'" :to="to" class="nad-card">
    <h3>{{ title }}</h3>
    <p v-if="meta" class="nad-card__meta">{{ meta }}</p>
    <p class="nad-card__body"><slot /></p>
  </component>
</template>

<style scoped>
.nad-card { display: block; border: 1px solid var(--nad-border); border-radius: 0.75rem; padding: 1.25rem; color: inherit; }
.nad-card:hover { border-color: var(--nad-accent); text-decoration: none; }
.nad-card h3 { margin: 0 0 0.25rem; font-size: 1.05rem; }
.nad-card__meta { margin: 0 0 0.5rem; color: var(--nad-muted); font-size: 0.85rem; }
.nad-card__body { margin: 0; color: var(--nad-muted); }
</style>
