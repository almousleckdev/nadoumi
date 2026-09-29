<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import StatCard from '@/components/dashboard/StatCard.vue'

interface ApplicantStats {
  total: number
  new30d: number
  incomplete: number
  active: number
  sampled: boolean
  sampledCount: number
}

defineProps<{
  stats: ApplicantStats | null
  state: 'ok' | 'loading' | 'error'
  error: string | null
}>()

const { t } = useI18n()
</script>

<template>
  <DashboardGroup
    :title="t('dashboard.groups.applicants')"
    :subtitle="t('dashboard.liveData')"
  >
    <StatCard
      :label="t('dashboard.k.totalApplicants')"
      :value="stats?.total ?? null"
      :state="state"
      :error-text="error ?? undefined"
    />
    <StatCard
      :label="t('dashboard.k.newApplicants')"
      :value="stats?.new30d ?? null"
      :hint="t('dashboard.last30d')"
      :state="state"
      :error-text="error ?? undefined"
    />
    <StatCard
      :label="t('dashboard.k.incompleteProfiles')"
      :value="stats?.incomplete ?? null"
      :hint="stats?.sampled ? t('dashboard.ofSample', { n: stats.sampledCount }) : undefined"
      :state="state"
      :error-text="error ?? undefined"
    />
    <StatCard
      :label="t('dashboard.k.activeApplicants')"
      :value="stats?.active ?? null"
      :state="state"
      :error-text="error ?? undefined"
    />
  </DashboardGroup>
</template>
