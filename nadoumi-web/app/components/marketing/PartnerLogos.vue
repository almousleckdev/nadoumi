<script setup lang="ts">
import type { UniversitySummary } from '~/types/catalog'

defineProps<{ universities: UniversitySummary[] }>()

const localePath = useLocalePath()
</script>

<template>
  <!-- One partner per slide; rendered inside a Carousel by the parent section. -->
  <NuxtLink
    v-for="u in universities"
    :key="u.id"
    :to="localePath(`/universities/${u.slug}`)"
    class="flex h-44 w-full flex-col items-center justify-center gap-3 rounded-xl border border-slate-200 bg-white p-6 no-underline transition-shadow hover:shadow-md"
    :title="u.name"
  >
    <img
      v-if="u.logoUrl || u.logoImageUrl"
      :src="(u.logoUrl || u.logoImageUrl) as string"
      :alt="u.name"
      loading="lazy"
      decoding="async"
      class="max-h-20 max-w-full object-contain"
    >
    <span class="text-center font-display text-base font-semibold text-slate-800">{{ u.name }}</span>
  </NuxtLink>
</template>
