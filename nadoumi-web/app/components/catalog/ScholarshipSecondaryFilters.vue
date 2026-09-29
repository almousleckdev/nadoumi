<script setup lang="ts">
import type { ScholarshipFacets } from '~/types/catalog'

const LEVELS = ['NON_DEGREE', 'DIPLOMA', 'BACHELOR', 'MASTER', 'PHD'] as const
const CATEGORIES = ['CSC', 'CGS', 'GOVERNMENT', 'PROVINCIAL', 'UNIVERSITY', 'PRESIDENTIAL', 'LANGUAGE', 'TYPE_A', 'TYPE_B', 'TYPE_C', 'TYPE_D'] as const

const props = defineProps<{
  level: string[]
  category: string[]
  facets: ScholarshipFacets | null
}>()

const emit = defineEmits<{ toggle: [key: 'level' | 'category', value: string] }>()

const { t } = useI18n()

function count(dim: keyof ScholarshipFacets, value: string): number {
  return props.facets?.[dim].find(b => b.value === value)?.count ?? 0
}
</script>

<template>
<details class="rounded-xl border border-slate-200 bg-white p-4">
  <summary class="cursor-pointer text-sm font-medium text-slate-700">{{ t('scholarships.moreFilters') }}</summary>
  <div class="mt-4 grid gap-6 sm:grid-cols-2">
    <fieldset>
      <legend class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.fLevel') }}</legend>
      <div class="mt-2 flex flex-wrap gap-x-4 gap-y-2">
        <label v-for="lv in LEVELS" :key="lv" class="inline-flex items-center gap-1.5 text-sm text-slate-700">
          <input
            type="checkbox"
            :checked="level.includes(lv)"
            class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
            @change="emit('toggle', 'level', lv)"
          >
          {{ t(`scholarships.level.${lv}`) }}
          <span class="text-slate-400">({{ count('levels', lv) }})</span>
        </label>
      </div>
    </fieldset>
    <fieldset>
      <legend class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.fCategory') }}</legend>
      <div class="mt-2 flex flex-wrap gap-x-4 gap-y-2">
        <label v-for="c in CATEGORIES" :key="c" class="inline-flex items-center gap-1.5 text-sm text-slate-700">
          <input
            type="checkbox"
            :checked="category.includes(c)"
            class="rounded border-slate-300 text-brand-600 focus-visible:ring-brand-500"
            @change="emit('toggle', 'category', c)"
          >
          {{ t(`scholarships.category.${c}`, c) }}
          <span class="text-slate-400">({{ count('categories', c) }})</span>
        </label>
      </div>
    </fieldset>
  </div>
</details>
</template>
