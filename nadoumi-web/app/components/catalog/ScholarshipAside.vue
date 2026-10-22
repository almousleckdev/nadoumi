<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'
import { deadlineSummary, dualMoney, moneyOf } from '~/utils/scholarshipDisplay'

const props = defineProps<{ s: ScholarshipDetail }>()

const { t } = useI18n()
const deadline = computed(() => deadlineSummary(props.s.deadline, t))
</script>

<template>
  <DetailSidebarCard :title="t('scholarships.deadlineLabel')">
    <div class="flex items-start gap-3">
      <span class="mt-1 shrink-0 text-brand-600"><DashboardIcon name="calendar" :size="22" /></span>
      <div>
        <p class="font-display text-2xl font-bold text-slate-900">{{ deadline.text }}</p>
        <p v-if="!deadline.rolling" class="text-sm tabular-nums text-slate-500">{{ deadline.date }}</p>
      </div>
    </div>
  </DetailSidebarCard>

  <DetailSidebarCard v-if="s.stipends.length" :title="t('scholarships.stipend')">
    <ul class="space-y-4">
      <li v-for="st in s.stipends" :key="st.level">
        <p class="text-xs uppercase tracking-wide text-slate-500">{{ t(`scholarships.level.${st.level}`, st.level) }}</p>
        <p class="font-display text-lg font-semibold text-red-600">
          {{ dualMoney(st.amountRmb, st.amountUsd) }}
          <span class="text-sm font-normal text-slate-500">/ {{ t(`scholarships.freq.${st.frequency}`, st.frequency) }}</span>
        </p>
        <p v-if="st.durationMonths" class="text-sm text-slate-600">{{ t('scholarships.stipendDuration', { n: st.durationMonths }) }}</p>
        <p v-if="st.conditions" class="text-sm text-slate-500">{{ st.conditions }}</p>
      </li>
    </ul>
  </DetailSidebarCard>

  <DetailSidebarCard v-if="moneyOf(s.applicationFee) || moneyOf(s.serviceFee)" :title="t('scholarships.upfront')">
    <PriceRows
      :rows="[
        ...(moneyOf(s.applicationFee) ? [{ label: t('scholarships.fee.APPLICATION'), price: moneyOf(s.applicationFee)! }] : []),
        ...(moneyOf(s.serviceFee) ? [{ label: t('scholarships.fee.NADOUMI_SERVICE'), price: moneyOf(s.serviceFee)! }] : []),
      ]"
    />
  </DetailSidebarCard>

  <DetailSidebarCard v-if="s.intakes.length" :title="t('scholarships.intakes')">
    <ul class="space-y-2 text-sm text-slate-700">
      <li v-for="(it, i) in s.intakes" :key="i" class="flex flex-wrap justify-between gap-x-3">
        <span class="font-medium text-slate-900">{{ t(`scholarships.intake.${it.term}`, it.term) }}</span>
        <span v-if="it.applicationClose" class="text-slate-500">{{ t('scholarships.closes') }} {{ it.applicationClose }}</span>
      </li>
    </ul>
  </DetailSidebarCard>
</template>
