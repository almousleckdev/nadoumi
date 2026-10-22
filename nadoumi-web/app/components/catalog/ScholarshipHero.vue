<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'

const props = defineProps<{ s: ScholarshipDetail, applyTo: string }>()

const { t } = useI18n()
const localePath = useLocalePath()

const backdrop = computed(() => props.s.heroUrl ?? props.s.heroImageUrl ?? props.s.coverUrl ?? props.s.coverImageUrl)
const cover = computed(() => props.s.coverUrl ?? props.s.coverImageUrl ?? props.s.heroUrl ?? props.s.heroImageUrl)
const place = computed(() => [props.s.city, props.s.province, props.s.country].filter(Boolean).join(', '))
</script>

<template>
<section class="relative isolate overflow-hidden bg-slate-900 text-white">
  <img
    v-if="backdrop"
    :src="mediaUrl(backdrop)"
    alt=""
    class="absolute inset-0 -z-20 h-full w-full object-cover opacity-30"
  >
  <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-900 via-slate-900/90 to-brand-900/60" aria-hidden="true" />
  <NContainer>
    <div class="grid items-center gap-8 py-10 sm:py-14 lg:grid-cols-[minmax(0,1fr)_24rem] lg:gap-12 lg:py-16">
      <div class="min-w-0">
        <nav class="mb-4 text-sm">
          <NuxtLink :to="localePath('/scholarships')" class="text-white/70 no-underline hover:text-white">
            <span aria-hidden="true">←</span> {{ t('catalog.backToList') }}
          </NuxtLink>
        </nav>
        <div class="flex flex-wrap items-center gap-2 text-xs font-semibold">
          <span class="rounded-full bg-white/10 px-2.5 py-1 ring-1 ring-white/20">{{ t(`scholarships.funding.${s.fundingModel}`) }}</span>
          <span v-if="s.hasStipend" class="rounded-full bg-white/10 px-2.5 py-1 ring-1 ring-white/20">{{ t('scholarships.withStipend') }}</span>
          <span v-if="s.hot" class="rounded-full bg-amber-400 px-2.5 py-1 text-amber-950">{{ t('scholarships.hot') }}</span>
          <span v-if="s.featured" class="rounded-full bg-brand-500 px-2.5 py-1">{{ t('catalog.featured') }}</span>
          <span v-if="s.recommended" class="rounded-full bg-white px-2.5 py-1 text-brand-800">{{ t('catalog.recommendedShort') }}</span>
          <span
            v-for="c in s.categories"
            :key="c"
            class="rounded-full bg-white/10 px-2.5 py-1 ring-1 ring-white/20"
          >{{ t(`scholarships.category.${c}`, c) }}</span>
        </div>
        <p v-if="s.referenceCode" class="mt-4 font-mono text-xs uppercase tracking-widest text-white/60">{{ s.referenceCode }}</p>
        <h1 class="mt-1 text-balance font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ s.title }}</h1>
        <p v-if="place" class="mt-2 text-white/70">{{ place }}</p>
        <p v-if="s.summary" class="mt-4 max-w-2xl text-base text-slate-200 sm:text-lg">{{ s.summary }}</p>
        <div class="mt-7 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <NButton :to="applyTo" size="lg" class="w-full sm:w-auto">{{ t('scholarships.applyNow') }}</NButton>
        </div>
        <p class="mt-2 text-xs text-slate-400">{{ t('scholarships.applyNote') }}</p>
      </div>

      <div
        v-if="cover"
        class="order-first overflow-hidden rounded-2xl bg-slate-800 shadow-xl ring-1 ring-white/15 lg:order-none"
      >
        <img
          :src="mediaUrl(cover)"
          :alt="s.title"
          class="aspect-[16/9] max-h-56 w-full object-cover sm:max-h-72 lg:max-h-none"
        >
      </div>
    </div>
  </NContainer>
</section>
</template>
