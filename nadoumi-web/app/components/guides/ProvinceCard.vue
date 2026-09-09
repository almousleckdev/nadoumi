<script setup lang="ts">
import type { Division } from '~/data/guides/china'
import { regionTint } from '~/data/guides/regions'

const props = defineProps<{ division: Division, regionId: string }>()
const tint = computed(() => regionTint(props.regionId))
</script>

<template>
  <article class="flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition-shadow hover:shadow-md">
    <!-- hero: real photograph where we have one, a designed graphic otherwise -->
    <div class="relative flex h-40 items-end overflow-hidden p-4" :style="{ background: tint.grad }">
      <NuxtImg
        v-if="division.hero"
        :src="division.hero"
        :alt="`${division.name}, China`"
        sizes="sm:100vw md:50vw lg:33vw"
        class="absolute inset-0 h-full w-full object-cover"
        :modifiers="{ fit: 'crop', auto: 'format' }"
      />
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

      <div class="absolute inset-0" :class="division.hero ? 'bg-gradient-to-t from-slate-950/80 via-slate-950/25 to-transparent' : ''" aria-hidden="true" />
      <span class="pointer-events-none absolute -right-1 top-1 font-display text-7xl font-black leading-none text-white/20">{{ division.cn }}</span>

      <div class="relative">
        <p class="text-[11px] font-semibold uppercase tracking-wide text-white/75">{{ division.kind }}</p>
        <h3 class="font-display text-2xl font-bold text-white drop-shadow-sm">{{ division.name }}</h3>
      </div>
    </div>

    <div class="flex flex-1 flex-col p-5">
      <p class="text-sm leading-6 text-slate-600">{{ division.overview }}</p>
      <p class="mt-2 text-sm leading-6 text-slate-600">{{ division.culture }}</p>

      <div class="mt-4 grid flex-1 gap-3 sm:grid-cols-2">
        <CityCard v-for="c in division.cities" :key="c.name" :city="c" :tint="tint" />
      </div>
    </div>
  </article>
</template>
