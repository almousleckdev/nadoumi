<script setup lang="ts">
defineProps<{ title?: string }>()
</script>

<template>
  <section class="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
    <h2 v-if="title" class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500">{{ title }}</h2>
    <slot />
  </section>
</template>
