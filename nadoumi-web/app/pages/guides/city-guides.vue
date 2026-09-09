<script setup lang="ts">
import { CHINA_REGIONS, CHINA_DIVISION_COUNT } from '~/data/guides/china'

const localePath = useLocalePath()
useSeo(
  'City Guides, Studying across China',
  'A guide to every province and region of China: its history, culture, language and festivals, with two cities and their leading universities.',
)
</script>

<template>
  <div>
    <PageHero
      title="City Guides"
      subtitle="Every province and region of China, history, culture, language and festivals, with two cities and their leading universities."
    >
      <nav class="mt-4 text-sm">
        <NuxtLink :to="localePath('/guides')" class="text-brand-700 no-underline hover:underline">Guides</NuxtLink>
        <span class="mx-2 text-slate-400" aria-hidden="true">/</span>
        <span class="text-slate-500">City Guides</span>
      </nav>
    </PageHero>

    <NContainer>
      <div class="grid gap-10 py-12 lg:grid-cols-[15rem_minmax(0,1fr)] lg:gap-14 sm:py-16">
        <aside class="lg:sticky lg:top-24 lg:self-start">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">Regions</p>
          <ul class="mt-3 space-y-1.5 text-sm">
            <li v-for="r in CHINA_REGIONS" :key="r.id">
              <a :href="`#${r.id}`" class="text-slate-600 no-underline hover:text-brand-700">{{ r.name }}</a>
            </li>
          </ul>
          <p class="mt-6 text-xs text-slate-400">{{ CHINA_DIVISION_COUNT }} provinces &amp; regions</p>
        </aside>

        <div class="space-y-14">
          <p class="max-w-2xl leading-7 text-slate-700">
            China is vast and varied, each province has its own dialects, cuisine, festivals and
            character, and its own universities. Use this to get a feel for where you might study,
            then explore the institutions on our
            <NuxtLink :to="localePath('/universities')">Universities</NuxtLink> page.
          </p>

          <section v-for="r in CHINA_REGIONS" :id="r.id" :key="r.id" class="scroll-mt-24">
            <h2 class="font-display text-2xl font-bold text-slate-900">{{ r.name }}</h2>
            <p class="mt-2 max-w-2xl leading-7 text-slate-600">{{ r.blurb }}</p>
            <div class="mt-6 space-y-6">
              <ProvinceGuide v-for="d in r.divisions" :key="d.name" :division="d" />
            </div>
          </section>

          <p class="border-t border-slate-200 pt-8 text-sm text-slate-500">
            University lists highlight a few leading institutions in each city and are not exhaustive.
            Administrative descriptions follow the People's Republic of China.
          </p>
        </div>
      </div>
    </NContainer>
  </div>
</template>
