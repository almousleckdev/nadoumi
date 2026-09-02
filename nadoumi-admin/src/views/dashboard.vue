<template>
  <div class="nad-page dash">
    <PageHeader
      :title="t('dashboard.title')"
      :subtitle="t('dashboard.subtitle')"
    >
      <template #actions>
        <el-button
          :loading="loading"
          :icon="Refresh"
          @click="refresh"
        >
          {{ t('dashboard.refresh') }}
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="userStore.mustChangePassword"
      :title="t('profile.initialWarning')"
      type="warning"
      show-icon
      :closable="false"
      class="dash__row"
    >
      <el-button
        size="small"
        type="primary"
        @click="router.push('/profile')"
      >
        {{ t('profile.changePassword') }}
      </el-button>
    </el-alert>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="dash__row"
    >
      <el-button
        size="small"
        @click="refresh"
      >
        {{ t('dashboard.retry') }}
      </el-button>
    </el-alert>

    <!-- Applicants — real data from /api/staff/applicants (interim source, ADMIN_ARCHITECTURE §6.1) -->
    <DashboardGroup
      :title="t('dashboard.groups.applicants')"
      :subtitle="t('dashboard.liveData')"
    >
      <StatCard
        :label="t('dashboard.k.totalApplicants')"
        :value="stats?.total ?? null"
        :state="cardState"
        :error-text="error ?? undefined"
      />
      <StatCard
        :label="t('dashboard.k.newApplicants')"
        :value="stats?.new30d ?? null"
        :hint="t('dashboard.last30d')"
        :state="cardState"
        :error-text="error ?? undefined"
      />
      <StatCard
        :label="t('dashboard.k.incompleteProfiles')"
        :value="stats?.incomplete ?? null"
        :hint="stats?.sampled ? t('dashboard.ofSample', { n: stats.sampledCount }) : undefined"
        :state="cardState"
        :error-text="error ?? undefined"
      />
      <StatCard
        :label="t('dashboard.k.activeApplicants')"
        :value="stats?.active ?? null"
        :state="cardState"
        :error-text="error ?? undefined"
      />
    </DashboardGroup>

    <!-- Profile completion (real) + recent applicants (real) -->
    <div class="dash__split">
      <DonutStat
        :label="t('dashboard.profileCompletion')"
        :segments="completionSegments"
        :center-value="completionPct"
        :center-caption="t('dashboard.complete')"
        :state="cardState"
        :error-text="error ?? undefined"
      />
      <RecentApplicants
        :rows="stats?.recent ?? []"
        :loading="loading && !stats"
        :error="error"
      />
    </div>

    <!-- Domains without a backend yet — honest coming-soon, never fake numbers -->
    <DashboardGroup :title="t('dashboard.groups.notYet')">
      <ComingSoonCard
        v-for="c in comingSoon"
        :key="c.key"
        :label="t(`dashboard.k.${c.key}`)"
        :domain="c.domain"
      />
    </DashboardGroup>

    <!-- Activity & operational tasks — need the Reporting event store -->
    <div class="dash__split">
      <el-card
        shadow="never"
        class="dash__panel"
      >
        <template #header>
          <span class="dash__panel-title">{{ t('dashboard.recentActivity') }}</span>
        </template>
        <EmptyState
          :title="t('dashboard.comingSoon')"
          :description="t('dashboard.activityComingSoon')"
          icon="Bell"
        />
      </el-card>
      <el-card
        shadow="never"
        class="dash__panel"
      >
        <template #header>
          <span class="dash__panel-title">{{ t('dashboard.pendingTasks') }}</span>
        </template>
        <EmptyState
          :title="t('dashboard.comingSoon')"
          :description="t('dashboard.tasksComingSoon')"
          icon="List"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useApplicantStats } from '@/composables/useApplicantStats'
import PageHeader from '@/components/PageHeader.vue'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import ComingSoonCard from '@/components/dashboard/ComingSoonCard.vue'
import DonutStat from '@/components/dashboard/DonutStat.vue'
import RecentApplicants from '@/components/dashboard/RecentApplicants.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const { data: stats, loading, error, refresh } = useApplicantStats()

const cardState = computed<'ok' | 'loading' | 'error'>(() =>
  error.value ? 'error' : (loading.value && !stats.value ? 'loading' : 'ok'))

const completionSegments = computed(() => [
  { label: t('dashboard.complete'), value: stats.value?.complete ?? 0, color: 'var(--nad-brand-500)' },
  { label: t('dashboard.k.incompleteProfiles'), value: stats.value?.incomplete ?? 0, color: '#cbd5e1' },
])
const completionPct = computed(() => {
  const s = stats.value
  if (!s || s.sampledCount === 0) return '—'
  return `${Math.round((s.complete / s.sampledCount) * 100)}%`
})

const comingSoon = [
  { key: 'applications', domain: 'Application' },
  { key: 'revenue', domain: 'Finance' },
  { key: 'expenses', domain: 'Finance' },
  { key: 'netEarnings', domain: 'Finance' },
  { key: 'payments', domain: 'Payment' },
  { key: 'employees', domain: 'Employee' },
  { key: 'payroll', domain: 'Payroll' },
  { key: 'notifications', domain: 'Notification' },
] as const

onMounted(refresh)
</script>

<style scoped>
.dash__row {
  margin-bottom: 16px;
}
.dash__split {
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 16px;
  margin-bottom: 28px;
}
.dash__panel {
  border: 1px solid var(--nad-line);
  border-radius: var(--nad-radius);
}
.dash__panel-title {
  font-weight: 650;
  font-size: 14px;
  color: var(--nad-ink);
}
@media (max-width: 900px) {
  .dash__split {
    grid-template-columns: 1fr;
  }
}
</style>
