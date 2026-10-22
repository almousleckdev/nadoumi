<script setup lang="ts">
defineProps<{ title?: string, description?: string }>()
</script>

<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs sm:p-6">
    <h2 v-if="title" class="font-display text-lg font-semibold text-slate-900 sm:text-xl">{{ title }}</h2>
    <p v-if="description" class="mt-1 text-sm text-slate-500">{{ description }}</p>
    <div :class="title ? 'mt-4' : ''">
      <slot />
    </div>
  </section>
</template>
