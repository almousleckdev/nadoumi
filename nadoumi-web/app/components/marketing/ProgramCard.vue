<script setup lang="ts">
import type { ProgramCard } from '~/types/catalog'

const props = withDefaults(defineProps<{
  program: ProgramCard
  variant?: 'carousel' | 'grid'
  showUniversity?: boolean
}>(), { variant: 'grid', showUniversity: true })

const { t } = useI18n()
const localePath = useLocalePath()

const tuition = computed(() => {
  const p = props.program
  return p.tuitionAmount != null
    ? `${p.tuitionCurrency ?? ''} ${p.tuitionAmount.toLocaleString('en')}`.trim()
    : ''
})
</script>

<template>
  <NuxtLink
    :to="localePath(`/programs/${program.id}`)"
    class="group flex flex-col rounded-xl border border-slate-200 bg-white p-5 no-underline shadow-xs transition-shadow hover:shadow-md"
    :class="variant === 'carousel' ? 'w-[19rem] sm:w-[21rem]' : 'w-full'"
  >
    <div class="flex flex-wrap items-center gap-2">
      <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
        {{ t(`program.level.${program.programType}`) }}
      </span>
      <span
        v-if="program.teachingLanguage"
        class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700"
      >
        {{ t(`program.lang.${program.teachingLanguage}`) }}
      </span>
      <span v-if="program.hot" class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-800">{{ t('program.hot') }}</span>
      <span v-else-if="program.featured" class="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-800">{{ t('catalog.featured') }}</span>
    </div>

    <h3 class="mt-3 font-display text-base font-semibold text-slate-900 group-hover:text-brand-800">
      {{ program.name }}
    </h3>
    <p v-if="program.nameCn" class="mt-0.5 text-sm text-slate-400">{{ program.nameCn }}</p>
    <p v-if="showUniversity && program.universityName" class="mt-1 text-sm text-slate-600">
      {{ t('program.offeredBy', { university: program.universityName }) }}
    </p>
    <p v-if="program.field" class="mt-1 text-sm text-slate-500">{{ program.field }}</p>

    <div class="mt-auto flex items-center justify-between pt-4 text-sm">
      <span class="text-slate-500">
        <template v-if="program.durationMonths">{{ t('program.duration', { months: program.durationMonths }) }}</template>
        <template v-else-if="tuition">{{ tuition }}</template>
      </span>
      <span class="inline-flex items-center gap-1 font-medium text-brand-700 group-hover:gap-2">
        {{ t('program.view') }}<span aria-hidden="true">→</span>
      </span>
    </div>
  </NuxtLink>
</template>
