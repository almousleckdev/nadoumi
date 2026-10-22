<script setup lang="ts">
import type { ProgramOffering } from '~/types/catalog'
import { formatDuration, groupOfferings, offeringMoney } from '~/utils/programDisplay'

const props = defineProps<{ offerings: ProgramOffering[] }>()

const { t } = useI18n()
const groups = computed(() => groupOfferings(props.offerings))

function tuition(o: ProgramOffering) {
  return offeringMoney(o.tuitionAmount, o.tuitionAmountUsd, o.tuitionCurrency)
}
function accommodation(o: ProgramOffering) {
  return offeringMoney(o.accommodationAmount, o.accommodationAmountUsd, o.accommodationCurrency)
}
</script>

<template>
  <DetailCard v-if="offerings.length" :title="t('program.offerings.title')" :description="t('program.offerings.description')">
    <div class="space-y-6">
      <section v-for="g in groups" :key="g.level ?? 'programme'">
        <h3 v-if="g.level" class="font-display text-base font-semibold text-slate-900">{{ t(`program.level.${g.level}`) }}</h3>
        <ul class="mt-3 divide-y divide-slate-100 rounded-xl border border-slate-200">
          <li v-for="o in g.offerings" :key="o.id" class="grid gap-3 p-4 sm:grid-cols-3 sm:gap-6">
            <div>
              <p class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('program.offerings.duration') }}</p>
              <p class="mt-0.5 font-display font-semibold text-slate-900">{{ formatDuration(o.durationMonths, t) }}</p>
            </div>
            <div>
              <p class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('program.offerings.tuition') }}</p>
              <template v-if="tuition(o)">
                <p class="mt-0.5 font-semibold tabular-nums text-red-600">{{ tuition(o) }}</p>
                <p class="text-xs text-slate-500">{{ t(`program.offerings.period.${o.tuitionPeriod}`) }}</p>
              </template>
              <p v-else class="mt-0.5 text-sm text-slate-500">{{ t('program.offerings.noPrice') }}</p>
            </div>
            <div>
              <p class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('program.offerings.accommodation') }}</p>
              <template v-if="accommodation(o)">
                <p class="mt-0.5 font-semibold tabular-nums text-red-600">{{ accommodation(o) }}</p>
                <p class="text-xs text-slate-500">{{ t(`program.offerings.period.${o.accommodationPeriod}`) }}</p>
              </template>
              <p v-else class="mt-0.5 text-sm text-slate-500">{{ t('program.offerings.noPrice') }}</p>
            </div>
            <p v-if="o.note" class="text-sm text-slate-600 sm:col-span-3">{{ o.note }}</p>
          </li>
        </ul>
      </section>
    </div>
  </DetailCard>
</template>
