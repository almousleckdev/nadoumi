<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import type { useFinanceOverview } from './useFinanceOverview'

defineProps<{ overview: ReturnType<typeof useFinanceOverview> }>()

const { t } = useI18n()
</script>

<template>
  <DashboardGroup
    :title="t('dashboard.groups.finance')"
    :subtitle="t('dashboard.thisYear')"
  >
    <StatCard
      :label="t('dashboard.k.revenueToday')"
      :value="overview.revenueToday.value.text"
      :hint="t('dashboard.today')"
      :state="overview.state.value"
    />
    <StatCard
      :label="t('dashboard.k.revenueTotal')"
      :value="overview.revenueAllTime.value.text"
      :hint="t('dashboard.allTime')"
      :state="overview.state.value"
    />
    <StatCard
      :label="t('dashboard.k.revenue')"
      :value="overview.summary.value.revenue"
      :hint="overview.summary.value.currency"
      :state="overview.state.value"
    />
    <StatCard
      :label="t('dashboard.k.expenses')"
      :value="overview.summary.value.expenses"
      :hint="overview.summary.value.currency"
      :state="overview.state.value"
    />
    <StatCard
      :label="t('dashboard.k.netEarnings')"
      :value="overview.summary.value.net"
      :hint="overview.summary.value.currency"
      :state="overview.state.value"
    />
    <StatCard
      :label="t('dashboard.k.netMargin')"
      :value="overview.summary.value.margin"
      :state="overview.state.value"
    />
  </DashboardGroup>
</template>
