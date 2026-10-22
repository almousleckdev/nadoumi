<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'

const props = defineProps<{ program: ProgramDetail, applyTo: string, universityTo: string }>()

const { t } = useI18n()
const localePath = useLocalePath()

const levelLabels = computed(() => props.program.programType === 'DEGREE' && props.program.levels?.length
  ? props.program.levels.map(l => t(`program.level.${l}`))
  : [t(`program.level.${props.program.programType}`)])
</script>

<template>
  <section class="relative isolate overflow-hidden bg-slate-900 text-white">
    <img
      v-if="program.imageUrl"
      :src="mediaUrl(program.imageUrl)"
      alt=""
      class="absolute inset-0 -z-20 h-full w-full object-cover opacity-30"
    >
    <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-brand-900/70" aria-hidden="true" />
    <NContainer>
      <div class="grid items-center gap-8 py-10 sm:py-14 lg:grid-cols-[minmax(0,1fr)_24rem] lg:gap-12 lg:py-16">
        <div class="min-w-0">
          <nav class="mb-4 text-sm">
            <NuxtLink :to="localePath('/programs')" class="text-white/70 no-underline hover:text-white">
              <span aria-hidden="true">←</span> {{ t('program.viewAll') }}
            </NuxtLink>
          </nav>
          <div class="flex flex-wrap items-center gap-2">
            <span
              v-for="lv in levelLabels"
              :key="lv"
              class="rounded-full bg-white/10 px-2.5 py-1 text-xs font-semibold ring-1 ring-white/20"
            >{{ lv }}</span>
            <span
              v-if="program.teachingLanguage"
              class="rounded-full bg-white/10 px-2.5 py-1 text-xs font-medium ring-1 ring-white/20"
            >{{ t(`program.lang.${program.teachingLanguage}`) }}</span>
            <span v-if="program.hot" class="rounded-full bg-amber-400 px-2.5 py-1 text-xs font-semibold text-amber-950">{{ t('program.hot') }}</span>
            <span v-if="program.featured" class="rounded-full bg-brand-500 px-2.5 py-1 text-xs font-semibold">{{ t('catalog.featured') }}</span>
          </div>
          <h1 class="mt-4 text-balance font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ program.name }}</h1>
          <p v-if="program.nameCn" class="mt-1 text-lg text-white/70">{{ program.nameCn }}</p>
          <NuxtLink
            v-if="program.universityName"
            :to="universityTo"
            class="mt-4 inline-flex items-center gap-1.5 text-sm font-medium text-white/90 no-underline hover:text-white"
          >
            {{ t('program.offeredBy', { university: program.universityName }) }}
            <span aria-hidden="true">→</span>
          </NuxtLink>
          <div class="mt-7 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
            <NButton :to="applyTo" size="lg" class="w-full sm:w-auto">{{ t('program.applyNow') }}</NButton>
            <NButton :to="universityTo" variant="secondary" size="lg" class="w-full sm:w-auto">{{ t('program.view') }}</NButton>
          </div>
        </div>

        <div
          v-if="program.imageUrl"
          class="order-first overflow-hidden rounded-2xl bg-slate-800 shadow-xl ring-1 ring-white/15 lg:order-none"
        >
          <img
            :src="mediaUrl(program.imageUrl)"
            :alt="program.name"
            class="aspect-[16/9] max-h-56 w-full object-cover sm:max-h-72 lg:max-h-none"
          >
        </div>
      </div>
    </NContainer>
  </section>
</template>
