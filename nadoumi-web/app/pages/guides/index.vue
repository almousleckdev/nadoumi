<script setup lang="ts">
import { GUIDES } from '~/data/guides'

const localePath = useLocalePath()
useSeo('Nadoumi Guides', 'Practical guides to studying in China, applying, scholarships, visas, daily life and a guide to every region.')
</script>

<template>
  <div>
    <PageHero
      title="Guides"
      subtitle="Everything you need before and after you apply to study in China, written plainly, kept up to date."
    />
    <NContainer>
      <div class="grid gap-5 py-12 sm:grid-cols-2 lg:grid-cols-3 sm:py-16">
        <NuxtLink
          v-for="g in GUIDES"
          :key="g.slug"
          :to="localePath(`/guides/${g.slug}`)"
          class="group flex flex-col rounded-2xl border border-slate-200 bg-white p-6 no-underline transition hover:border-brand-300 hover:shadow-sm"
        >
          <p class="font-display text-lg font-bold text-slate-900 group-hover:text-brand-700">{{ g.title }}</p>
          <p class="mt-2 text-sm leading-6 text-slate-600">{{ g.summary }}</p>
          <span class="mt-4 text-sm font-medium text-brand-700">Read &rarr;</span>
        </NuxtLink>
      </div>
    </NContainer>
  </div>
</template>
