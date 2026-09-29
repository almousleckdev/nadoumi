<script setup lang="ts">
import type { ScholarshipCard } from '~/types/catalog'
import type { DeadlineTone } from '~/utils/deadline'

defineProps<{
  items: ScholarshipCard[]
  pending: boolean
  error: unknown
  isEmpty: boolean
  hasActiveFilters: boolean
}>()

const emit = defineEmits<{ refresh: [], clear: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()

function levelLabel(code: string) {
  return t(`scholarships.level.${code}`)
}
function fundingLabel(model: string) {
  return t(`scholarships.funding.${model}`)
}
function nextIntake(s: ScholarshipCard) {
  return s.intakes[0] ? t(`scholarships.intake.${s.intakes[0].term}`, s.intakes[0].term) : ''
}
function place(s: ScholarshipCard) {
  return [s.city, s.country].filter(Boolean).join(', ')
}
function fee(s: ScholarshipCard) {
  return s.applicationFee ?? s.serviceFee ?? null
}
function deadlineDays(s: ScholarshipCard): number | null {
  return daysUntilDeadline(s.deadline)
}
const DEADLINE_TONE_CLASS: Record<DeadlineTone, string> = {
  passed: 'bg-slate-100 text-slate-500',
  urgent: 'bg-red-100 text-red-700',
  soon: 'bg-amber-100 text-amber-700',
  ok: 'bg-emerald-100 text-emerald-700',
}
function deadlineClass(s: ScholarshipCard): string {
  return DEADLINE_TONE_CLASS[deadlineTone(deadlineDays(s))]
}
</script>

<template>
<div class="overflow-x-auto rounded-xl border border-slate-200">
  <table class="w-full min-w-[52rem] text-left text-sm">
    <thead class="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
      <tr>
        <th class="px-4 py-3 font-medium">{{ t('scholarships.colTitle') }}</th>
        <th class="px-4 py-3 font-medium">{{ t('scholarships.colLocation') }}</th>
        <th class="px-4 py-3 font-medium">{{ t('scholarships.colLevel') }}</th>
        <th class="px-4 py-3 font-medium">{{ t('scholarships.colIntake') }}</th>
        <th class="px-4 py-3 font-medium">{{ t('scholarships.colFunding') }}</th>
        <th class="px-4 py-3 text-right font-medium">{{ t('scholarships.colFees') }}</th>
        <th class="px-4 py-3 font-medium">{{ t('scholarships.colDeadline') }}</th>
        <th class="px-4 py-3" />
      </tr>
    </thead>
    <tbody class="divide-y divide-slate-100">
      <template v-if="pending">
        <tr v-for="n in 6" :key="n">
          <td v-for="c in 8" :key="c" class="px-4 py-3">
            <NSkeleton class="h-3.5 rounded" :style="{ width: c === 8 ? '1.5rem' : '80%' }" />
          </td>
        </tr>
      </template>
      <tr v-else-if="error">
        <td colspan="8" class="px-4 py-8 text-center">
          <NAlert tone="danger">
            {{ t('errors.loadSection') }}
            <button type="button" class="ms-2 font-medium underline" @click="emit('refresh')">{{ t('common.retry') }}</button>
          </NAlert>
        </td>
      </tr>
      <tr v-else-if="isEmpty">
        <td colspan="8" class="px-4 py-12 text-center">
          <p class="font-display text-base font-semibold text-slate-900">
            {{ hasActiveFilters ? t('catalog.noResultsTitle') : t('scholarships.noneTitle') }}
          </p>
          <p class="mt-1 text-slate-600">
            {{ hasActiveFilters ? t('catalog.noResultsBody') : t('scholarships.noneBody') }}
          </p>
          <button v-if="hasActiveFilters" type="button" class="mt-3 text-sm font-medium text-brand-700" @click="emit('clear')">
            {{ t('catalog.clearFilters') }}
          </button>
        </td>
      </tr>
      <tr v-for="s in items" v-else :key="s.id" class="align-top hover:bg-slate-50/60">
        <td class="px-4 py-3">
          <NuxtLink :to="localePath(`/scholarships/${s.slug}`)" class="font-medium text-slate-900 hover:text-brand-800">
            {{ s.title }}
          </NuxtLink>
          <span v-if="s.hot" class="ms-2 rounded-full bg-amber-100 px-1.5 py-0.5 text-[0.65rem] font-semibold text-amber-800">{{ t('scholarships.hot') }}</span>
          <span v-else-if="s.featured" class="ms-2 rounded-full bg-brand-100 px-1.5 py-0.5 text-[0.65rem] font-semibold text-brand-800">{{ t('catalog.featured') }}</span>
          <span v-if="s.referenceCode" class="mt-0.5 block font-mono text-[0.7rem] text-slate-400">{{ s.referenceCode }}</span>
        </td>
        <td class="whitespace-nowrap px-4 py-3 text-slate-600">{{ place(s) || '' }}</td>
        <td class="px-4 py-3 text-slate-600">{{ s.levels.map(levelLabel).join(', ') || '' }}</td>
        <td class="whitespace-nowrap px-4 py-3 text-slate-600">{{ nextIntake(s) || t('catalog.rollingDeadline') }}</td>
        <td class="whitespace-nowrap px-4 py-3">
          <span class="text-slate-700">{{ fundingLabel(s.fundingModel) }}</span>
          <span v-if="s.hasStipend" class="ms-1 text-emerald-600" :title="t('scholarships.withStipend')">●</span>
        </td>
        <td class="whitespace-nowrap px-4 py-3 text-right">
          <template v-if="fee(s)">
            <span class="block font-semibold tabular-nums text-red-600">¥{{ fee(s)!.amountRmb.toLocaleString('en') }}</span>
            <span class="block text-xs tabular-nums text-red-400">${{ fee(s)!.amountUsd.toLocaleString('en') }}</span>
          </template>
          <span v-else-if="s.fundingModel === 'FULLY'" class="text-emerald-600">{{ t('scholarships.feesNone') }}</span>
          <span v-else class="text-slate-300">·</span>
        </td>
        <td class="whitespace-nowrap px-4 py-3">
          <span
            v-if="deadlineDays(s) != null"
            class="inline-flex rounded-full px-2 py-0.5 text-xs font-semibold"
            :class="deadlineClass(s)"
          >{{ s.deadline }}</span>
          <span v-else class="text-xs text-slate-400">{{ t('scholarships.deadlineRolling') }}</span>
        </td>
        <td class="px-4 py-3 text-right">
          <NButton :to="localePath(`/scholarships/${s.slug}`)" size="sm">{{ t('scholarships.apply') }}</NButton>
        </td>
      </tr>
    </tbody>
  </table>
</div>
</template>
