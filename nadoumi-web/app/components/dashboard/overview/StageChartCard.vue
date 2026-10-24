<script setup lang="ts">
import type { StageCount } from '~/utils/applicationView'

const props = defineProps<{ distribution: StageCount[], pending: boolean }>()

const { t } = useI18n()
const segments = computed(() => props.distribution.map(d => ({
  key: d.status, label: t(`dashboard.applications.status.${d.status}`), value: d.count,
})))
const total = computed(() => props.distribution.reduce((sum, d) => sum + d.count, 0))
</script>

<template>
  <SectionCard :title="t('dashboard.home.chartTitle')">
    <NSkeleton v-if="pending" class="h-36 w-full rounded-lg" />
    <p v-else-if="!total" class="py-6 text-center text-sm text-slate-500" data-test="chart-empty">{{ t('dashboard.home.chartEmpty') }}</p>
    <DonutChart v-else :segments="segments">
      <span>
        <span class="block font-display text-3xl font-bold leading-none tabular-nums text-slate-900">{{ total }}</span>
        <span class="mt-1 block text-xs text-slate-500">{{ t('dashboard.home.chartTotal') }}</span>
      </span>
    </DonutChart>
  </SectionCard>
</template>
