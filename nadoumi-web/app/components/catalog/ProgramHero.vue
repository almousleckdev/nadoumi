<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'

const props = defineProps<{ program: ProgramDetail, applyTo: string, universityTo: string }>()

const { t } = useI18n()

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
      class="absolute inset-0 -z-20 h-full w-full object-cover opacity-35"
    >
    <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-brand-900/70" aria-hidden="true" />
    <NContainer>
      <div class="max-w-3xl py-14 sm:py-20">
        <div class="flex flex-wrap items-center gap-2">
          <span
            v-for="lv in levelLabels"
            :key="lv"
            class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-semibold ring-1 ring-white/20"
          >{{ lv }}</span>
          <span
            v-if="program.teachingLanguage"
            class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium ring-1 ring-white/20"
          >{{ t(`program.lang.${program.teachingLanguage}`) }}</span>
          <span v-if="program.hot" class="rounded-full bg-amber-400 px-2.5 py-0.5 text-xs font-semibold text-amber-950">{{ t('program.hot') }}</span>
        </div>
        <h1 class="mt-4 font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ program.name }}</h1>
        <p v-if="program.nameCn" class="mt-1 text-lg text-white/70">{{ program.nameCn }}</p>
        <NuxtLink
          v-if="program.universityName"
          :to="universityTo"
          class="mt-4 inline-flex items-center gap-1.5 text-sm font-medium text-white/90 hover:text-white"
        >
          {{ t('program.offeredBy', { university: program.universityName }) }}
          <span aria-hidden="true">→</span>
        </NuxtLink>
        <div class="mt-7 flex flex-wrap gap-3">
          <NButton :to="applyTo" size="lg">{{ t('program.applyNow') }}</NButton>
          <NButton :to="universityTo" variant="secondary" size="lg">{{ t('program.view') }}</NButton>
        </div>
      </div>
    </NContainer>
  </section>
</template>
