<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'
import { deadlineSummary, dualMoney, moneyOf } from '~/utils/scholarshipDisplay'

const props = defineProps<{ s: ScholarshipDetail }>()

const { t } = useI18n()

const deadline = computed(() => deadlineSummary(props.s.deadline, t))
</script>

<template>
<aside class="space-y-6 lg:sticky lg:top-24 lg:self-start">
  <div v-if="s.stipends.length" class="rounded-xl border border-slate-200 bg-white p-5">
    <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.stipend') }}</h2>
    <ul class="mt-2 space-y-3">
      <li v-for="st in s.stipends" :key="st.level">
        <p class="text-xs uppercase tracking-wide text-slate-400">{{ t(`scholarships.level.${st.level}`, st.level) }}</p>
        <p class="font-display text-lg font-semibold text-slate-900">
          {{ dualMoney(st.amountRmb, st.amountUsd) }}
          <span class="text-sm font-normal text-slate-500">/ {{ t(`scholarships.freq.${st.frequency}`, st.frequency) }}</span>
        </p>
        <p v-if="st.durationMonths" class="text-sm text-slate-600">{{ t('scholarships.stipendDuration', { n: st.durationMonths }) }}</p>
        <p v-if="st.conditions" class="text-sm text-slate-500">{{ st.conditions }}</p>
      </li>
    </ul>
  </div>

  <div v-if="moneyOf(s.applicationFee) || moneyOf(s.serviceFee)" class="rounded-xl border border-slate-200 bg-white p-5 text-sm">
    <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.upfront') }}</h2>
    <ul class="mt-3 space-y-1 text-slate-700">
      <li v-if="moneyOf(s.applicationFee)">{{ t('scholarships.fee.APPLICATION') }}: {{ moneyOf(s.applicationFee) }}</li>
      <li v-if="moneyOf(s.serviceFee)">{{ t('scholarships.fee.NADOUMI_SERVICE') }}: {{ moneyOf(s.serviceFee) }}</li>
    </ul>
  </div>

  <div v-if="s.intakes.length" class="rounded-xl border border-slate-200 bg-white p-5 text-sm">
    <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('scholarships.intakes') }}</h2>
    <ul class="mt-3 space-y-1 text-slate-700">
      <li v-for="(it, i) in s.intakes" :key="i">
        {{ t(`scholarships.intake.${it.term}`, it.term) }}
        <span v-if="it.applicationClose" class="text-slate-400"> · {{ t('scholarships.closes') }} {{ it.applicationClose }}</span>
      </li>
    </ul>
  </div>

  <!-- animated deadline card — sits below Upfront fee + Intakes -->
  <div
    class="deadline-banner relative overflow-hidden rounded-xl border p-5"
    :class="{
      'border-red-200 bg-red-50 text-red-900': deadline.tone === 'urgent' || deadline.tone === 'passed',
      'border-amber-200 bg-amber-50 text-amber-900': deadline.tone === 'soon',
      'border-emerald-200 bg-emerald-50 text-emerald-900': deadline.tone === 'ok',
    }"
  >
    <span class="deadline-banner__glow" aria-hidden="true" />
    <div class="relative">
      <div class="flex items-center gap-2">
        <span
          class="deadline-banner__pulse inline-block h-2.5 w-2.5 rounded-full"
          :class="{
            'bg-red-500': deadline.tone === 'urgent' || deadline.tone === 'passed',
            'bg-amber-500': deadline.tone === 'soon',
            'bg-emerald-500': deadline.tone === 'ok',
          }"
          aria-hidden="true"
        />
        <p class="text-xs font-semibold uppercase tracking-[0.14em] opacity-70">{{ t('scholarships.deadlineLabel') }}</p>
      </div>
      <p class="mt-1.5 font-display text-2xl font-bold tracking-tight">{{ deadline.text }}</p>
      <p v-if="!deadline.rolling" class="mt-0.5 text-sm tabular-nums opacity-70">{{ deadline.date }}</p>
    </div>
  </div>
</aside>
</template>

<style scoped>
.deadline-banner__glow {
  position: absolute;
  inset: -40% -10%;
  background: radial-gradient(60% 60% at 20% 30%, rgba(255, 255, 255, 0.55), transparent 70%);
  transform: translateX(-30%);
  animation: deadline-sweep 6s ease-in-out infinite;
}
.deadline-banner__pulse {
  animation: deadline-pulse 1.8s ease-in-out infinite;
}
@keyframes deadline-sweep {
  0%, 100% { transform: translateX(-30%); opacity: 0.6; }
  50% { transform: translateX(30%); opacity: 1; }
}
@keyframes deadline-pulse {
  0%, 100% { transform: scale(1); opacity: 1; box-shadow: 0 0 0 0 currentColor; }
  50% { transform: scale(1.35); opacity: 0.75; box-shadow: 0 0 0 6px transparent; }
}
@media (prefers-reduced-motion: reduce) {
  .deadline-banner__glow,
  .deadline-banner__pulse { animation: none; }
}
</style>
