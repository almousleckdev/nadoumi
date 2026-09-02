<script setup lang="ts">
defineProps<{ title: string, meta?: string, to?: string }>()
</script>

<template>
  <component
    :is="to ? 'NuxtLink' : 'div'"
    :to="to"
    class="block rounded-xl border border-slate-200 p-5 text-inherit no-underline transition-colors"
    :class="to ? 'hover:border-brand-500' : ''"
  >
    <h3 class="font-display text-base font-semibold text-slate-900">{{ title }}</h3>
    <p v-if="meta" class="mt-1 text-sm text-slate-500">{{ meta }}</p>
    <p class="mt-2 text-sm text-slate-600"><slot /></p>
  </component>
</template>
