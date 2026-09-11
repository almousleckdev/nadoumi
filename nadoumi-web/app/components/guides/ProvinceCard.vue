<script setup lang="ts">
import type { Division } from '~/data/guides/china'
import { regionTint } from '~/data/guides/regions'
import { provinceHero } from '~/data/guides/province-heroes'

const props = defineProps<{ division: Division, regionId: string }>()
const { t } = useI18n()
const tint = computed(() => regionTint(props.regionId))
const hero = computed(() => provinceHero(props.division.name))
const kindLabel = computed(() => t(`guides.cityGuides.kind.${props.division.kind}`))
const overview = computed(() => t(`guides.cityGuides.divisions.${props.division.slug}.overview`))
const culture = computed(() => t(`guides.cityGuides.divisions.${props.division.slug}.culture`))
</script>

<template>
  <article class="flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition-shadow hover:shadow-md">
    <!-- hero: real photograph where we have one, a designed graphic otherwise -->
    <div class="relative flex h-44 items-end overflow-hidden p-4" :style="{ background: tint.grad }">
      <img
        v-if="hero"
        :src="hero"
        :alt="`${division.name}, China`"
        loading="lazy"
        decoding="async"
        class="absolute inset-0 h-full w-full object-cover"
      >
      <svg
        v-else
        class="pointer-events-none absolute inset-0 h-full w-full opacity-[0.18]"
        viewBox="0 0 200 120"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <path d="M0 90 Q 30 60 60 78 T 120 66 T 200 84" fill="none" stroke="#fff" stroke-width="2" />
        <path d="M0 104 Q 40 80 80 96 T 160 84 T 200 100" fill="none" stroke="#fff" stroke-width="2" />
        <path d="M0 74 Q 50 44 100 62 T 200 58" fill="none" stroke="#fff" stroke-width="2" />
      </svg>

      <div
        class="absolute inset-0"
        :class="hero ? 'bg-gradient-to-t from-slate-950/85 via-slate-950/30 to-transparent' : ''"
        aria-hidden="true"
      />
      <span class="pointer-events-none absolute -right-1 top-1 font-display text-7xl font-black leading-none text-white/20">{{ division.cn }}</span>

      <div class="relative">
        <p class="text-[11px] font-semibold uppercase tracking-wide text-white/75">{{ kindLabel }}</p>
        <h3 class="font-display text-2xl font-bold text-white drop-shadow-sm">{{ division.name }}</h3>
      </div>
    </div>

    <div class="flex flex-1 flex-col p-5">
      <p class="text-sm leading-6 text-slate-600">{{ overview }}</p>
      <p class="mt-2 text-sm leading-6 text-slate-600">{{ culture }}</p>

      <div class="mt-4 grid flex-1 gap-3 sm:grid-cols-2">
        <CityCard v-for="c in division.cities" :key="c.id" :city="c" :division-slug="division.slug" :tint="tint" />
      </div>
    </div>
  </article>
</template>
