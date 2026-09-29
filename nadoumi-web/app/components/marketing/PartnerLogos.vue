<script setup lang="ts">
import type { UniversitySummary } from '~/types/catalog'

defineProps<{ universities: UniversitySummary[] }>()

const localePath = useLocalePath()
</script>

<template>
  <ul class="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
    <li
      v-for="u in universities"
      :key="u.id"
    >
      <NuxtLink
        :to="localePath(`/universities/${u.slug}`)"
        class="flex h-28 items-center justify-center rounded-xl border border-slate-200 bg-white p-5 no-underline transition-shadow hover:shadow-md"
        :title="u.name"
      >
        <img
          v-if="u.logoUrl || u.logoImageUrl"
          :src="(u.logoUrl || u.logoImageUrl) as string"
          :alt="u.name"
          loading="lazy"
          decoding="async"
          class="max-h-16 max-w-full object-contain"
        >
        <span
          v-else
          class="text-center text-sm font-semibold text-slate-700"
        >{{ u.name }}</span>
      </NuxtLink>
    </li>
  </ul>
</template>
