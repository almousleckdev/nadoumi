<script setup lang="ts">
import { GUIDES } from '~/data/guides'

const localePath = useLocalePath()
useSeo('Nadoumi Guides', 'Practical guides to studying in China, applying, scholarships, visas, daily life and a guide to every region.')
</script>

<template>
  <div>
    <PageHero
      title="Guides"
      subtitle="Everything you need before and after you apply to study in China, written plainly and kept up to date."
    />
    <NContainer>
      <div class="grid gap-6 py-12 sm:grid-cols-2 lg:grid-cols-3 sm:py-16">
        <NuxtLink
          v-for="g in GUIDES"
          :key="g.slug"
          :to="localePath(`/guides/${g.slug}`)"
          class="group flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white no-underline transition hover:border-brand-300 hover:shadow-md"
        >
          <div class="relative h-32 overflow-hidden bg-slate-100">
            <NuxtImg
              :src="g.hero"
              :alt="g.title"
              sizes="sm:100vw md:50vw lg:33vw"
              class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
              :modifiers="{ fit: 'crop', auto: 'format' }"
            />
            <span class="absolute bottom-0 left-4 translate-y-1/2 flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-white text-brand-700 shadow-sm">
              <GuideIcon :name="g.icon" :size="20" />
            </span>
          </div>
          <div class="flex flex-1 flex-col p-6 pt-8">
            <p class="font-display text-lg font-bold text-slate-900 group-hover:text-brand-700">{{ g.title }}</p>
            <p class="mt-2 flex-1 text-sm leading-6 text-slate-600">{{ g.summary }}</p>
            <span class="mt-4 text-sm font-medium text-brand-700">Read &rarr;</span>
          </div>
        </NuxtLink>
      </div>
    </NContainer>
  </div>
</template>
