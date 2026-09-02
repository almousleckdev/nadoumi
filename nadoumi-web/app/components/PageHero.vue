<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <section class="nad-hero">
    <div class="nad-container">
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
      <slot />
    </div>
  </section>
</template>

<style scoped>
.nad-hero { padding: 3.5rem 0 2rem; }
.nad-hero h1 { margin: 0 0 0.5rem; font-size: clamp(1.8rem, 4vw, 2.75rem); }
.nad-hero p { margin: 0; color: var(--nad-muted); max-width: 44rem; }
</style>
