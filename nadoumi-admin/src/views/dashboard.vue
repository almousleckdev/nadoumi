<template>
  <div class="nad-page dash">
    <el-alert
      v-if="userStore.mustChangePassword"
      :title="t('profile.initialWarning')"
      type="warning"
      show-icon
      :closable="false"
      class="dash__pw"
    >
      <el-button
        size="small"
        type="primary"
        @click="router.push('/profile')"
      >
        {{ t('profile.changePassword') }}
      </el-button>
    </el-alert>

    <header class="dash__head">
      <div>
        <h1 class="dash__title">
          {{ t('dashboard.title') }}
        </h1>
        <p class="dash__subtitle">
          {{ t('dashboard.subtitle') }}
        </p>
      </div>
      <el-button
        :loading="loading"
        :icon="Refresh"
        @click="refresh"
      >
        {{ t('dashboard.refresh') }}
      </el-button>
    </header>

    <!-- Applicants — real, from /api/staff/applicants (interim source until the Reporting slice; ADMIN_ARCHITECTURE §6) -->
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
        :hint="stats?.sampled ? t('dashboard.ofSample', { n: 200 }) : undefined"
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

    <!-- Applications — Application domain not built yet -->
    <DashboardGroup :title="t('dashboard.groups.applications')">
      <ComingSoonCard
        v-for="k in APPLICATION_KEYS"
        :key="k"
        :label="t(`dashboard.k.${k}`)"
        domain="Application"
      />
    </DashboardGroup>

    <!-- Finance — Finance / Payment domains not built yet -->
    <DashboardGroup :title="t('dashboard.groups.finance')">
      <ComingSoonCard
        :label="t('dashboard.k.revenue')"
        domain="Finance"
      />
      <ComingSoonCard
        :label="t('dashboard.k.expenses')"
        domain="Finance"
      />
      <ComingSoonCard
        :label="t('dashboard.k.netEarnings')"
        domain="Finance"
      />
      <ComingSoonCard
        :label="t('dashboard.k.outstandingPayments')"
        domain="Payment"
      />
    </DashboardGroup>

    <!-- Operations — Employee / Workflow / Reporting domains not built yet -->
    <DashboardGroup :title="t('dashboard.groups.operations')">
      <ComingSoonCard
        :label="t('dashboard.k.activeEmployees')"
        domain="Employee"
      />
      <ComingSoonCard
        :label="t('dashboard.k.pendingTasks')"
        domain="Workflow"
      />
      <ComingSoonCard
        :label="t('dashboard.k.operationalAlerts')"
        domain="Reporting"
      />
    </DashboardGroup>

    <div class="dash__lists">
      <RecentApplicants
        :rows="stats?.recent ?? []"
        :loading="loading"
        :error="error"
      />

      <el-card shadow="never">
        <template #header>
          {{ t('dashboard.recentActivity') }}
        </template>
        <el-empty
          :description="t('dashboard.comingSoonNote', { domain: 'Reporting' })"
          :image-size="72"
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
import StatCard from '@/components/dashboard/StatCard.vue'
import ComingSoonCard from '@/components/dashboard/ComingSoonCard.vue'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import RecentApplicants from '@/components/dashboard/RecentApplicants.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const { data: stats, loading, error, refresh } = useApplicantStats()

const cardState = computed<'ok' | 'loading' | 'error'>(() =>
  error.value ? 'error' : loading.value && !stats.value ? 'loading' : 'ok')

const APPLICATION_KEYS = [
  'totalApplications', 'newApplications', 'pendingReview', 'accepted', 'rejected',
] as const

onMounted(refresh)
</script>

<style scoped>
.dash__pw {
  margin-bottom: 16px;
}
.dash__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}
.dash__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}
.dash__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.dash__lists {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}
</style>
