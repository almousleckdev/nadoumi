<script setup lang="ts">
import type { ProgramCard } from '~/types/catalog'
import { formatDuration, offeringMoney } from '~/utils/programDisplay'

const props = withDefaults(defineProps<{
  program: ProgramCard
  variant?: 'carousel' | 'grid'
  showUniversity?: boolean
}>(), { variant: 'grid', showUniversity: true })

const { t } = useI18n()
const localePath = useLocalePath()

const tuition = computed(() =>
  offeringMoney(props.program.tuitionAmount, props.program.tuitionAmountUsd, props.program.tuitionCurrency))

const levelLabel = computed(() => {
  const p = props.program
  return p.programType === 'DEGREE' && p.levels?.length
    ? p.levels.map(l => t(`program.level.${l}`)).join(' · ')
    : t(`program.level.${p.programType}`)
})
</script>

<template>
  <NuxtLink
    :to="localePath(`/programs/${program.slug}`)"
    class="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white no-underline shadow-xs transition-shadow hover:shadow-md"
    :class="variant === 'carousel' ? 'w-[19rem] sm:w-[21rem]' : 'w-full'"
  >
    <CardMedia :src="program.imageUrl" :label="program.name">
      <CardBadge v-if="program.hot">{{ t('program.hot') }}</CardBadge>
      <CardBadge v-if="program.featured">{{ t('catalog.featured') }}</CardBadge>
    </CardMedia>
    <div class="flex flex-1 flex-col p-5">
      <div class="flex flex-wrap items-center gap-2">
        <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
          {{ levelLabel }}
        </span>
        <span
          v-if="program.teachingLanguage"
          class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700"
        >
          {{ t(`program.lang.${program.teachingLanguage}`) }}
        </span>
      </div>

      <h3 class="mt-3 font-display text-base font-semibold text-slate-900 group-hover:underline">
        {{ program.name }}
      </h3>
      <p v-if="program.nameCn" class="mt-0.5 text-sm text-slate-400">{{ program.nameCn }}</p>
      <p v-if="showUniversity && program.universityName" class="mt-1 text-sm text-slate-600">
        {{ t('program.offeredBy', { university: program.universityName }) }}
      </p>
      <p v-if="program.field" class="mt-1 text-sm text-slate-500">{{ program.field }}</p>

      <p v-if="tuition" class="mt-3 text-sm font-semibold tabular-nums text-red-600">
        <span class="font-normal text-slate-500">{{ t('program.tuitionFrom') }}</span> {{ tuition }}
      </p>
      <div class="mt-auto flex items-center justify-between pt-4 text-sm">
        <span class="text-slate-500">
          <template v-if="program.durationMonths">{{ formatDuration(program.durationMonths, t) }}</template>
        </span>
        <span class="inline-flex items-center gap-1 font-medium text-slate-900 group-hover:gap-2">
          {{ t('program.view') }}<span aria-hidden="true">→</span>
        </span>
      </div>
    </div>
  </NuxtLink>
</template>
