<template>
  <div class="nad-page dash">
    <PageHeader
      :title="t('dashboard.title')"
      :subtitle="t('dashboard.subtitle')"
    >
      <template #actions>
        <el-button
          :loading="loading || finance.loading.value"
          :icon="Refresh"
          @click="refreshAll"
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
        @click="refreshAll"
      >
        {{ t('dashboard.retry') }}
      </el-button>
    </el-alert>

    <el-card
      v-if="access.nothingVisible.value"
      shadow="never"
      class="dash__panel dash__row"
    >
      <EmptyState
        :title="t('dashboard.noAccessTitle')"
        :description="t('dashboard.noAccessBody')"
        icon="Lock"
      />
    </el-card>

    <PlatformGroup
      v-if="access.showPlatform.value"
      :access="access"
      :counts="platformCounts.counts.value"
      :applicant-total="stats?.total ?? null"
      :applicant-state="cardState"
    />

    <FinanceGroup
      v-if="access.canSeeFinance.value"
      :overview="finance"
    />

    <ApplicantsGroup
      v-if="access.canSeeApplicants.value"
      :stats="stats"
      :state="cardState"
      :error="error"
    />

    <div
      v-if="access.canSeeCatalog.value || access.canSeeApplicants.value"
      class="dash__split"
    >
      <DashboardPanel
        v-if="access.canSeeCatalog.value"
        :title="t('dashboard.popularUniversities')"
        :slices="catalog.data.value"
        :caption="t('dashboard.k.totalProgrammes')"
        :loading="catalog.loading.value"
        :empty-title="t('dashboard.noProgrammes')"
        empty-icon="Notebook"
      />
      <DashboardPanel
        v-if="access.canSeeApplicants.value"
        :title="t('dashboard.recentApplicants')"
        :slices="applicantChart"
        :caption="t('dashboard.byStatus')"
        :loading="loading && !stats"
        :empty-title="t('dashboard.noApplicants')"
        empty-icon="User"
        view-all-to="/applicants"
      />
    </div>

    <div
      v-if="!access.nothingVisible.value"
      class="dash__split"
    >
      <DashboardPanel
        v-if="access.canSeeTasks.value"
        :title="t('dashboard.myTasks')"
        :slices="taskChart"
        :caption="t('dashboard.byPriority')"
        :loading="tasks.loading.value"
        :empty-title="t('dashboard.noOpenTasks')"
        empty-icon="Select"
        view-all-to="/tasks"
      />
      <DashboardPanel
        :title="t('dashboard.recentActivity')"
        :slices="activityChart"
        :caption="t('dashboard.byType')"
        :loading="activity.loading.value"
        :empty-title="t('dashboard.noActivity')"
        empty-icon="Bell"
        view-all-to="/notifications"
      />
    </div>

    <DashboardGroup
      v-if="access.isOwner.value"
      :title="t('dashboard.groups.notYet')"
    >
      <ComingSoonCard
        v-for="c in comingSoon"
        :key="c.key"
        :label="t(`dashboard.k.${c.key}`)"
        :domain="c.domain"
      />
    </DashboardGroup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useApplicantStats } from '@/composables/useApplicantStats'
import { listTasks } from '@/api/hr'
import { listPrograms } from '@/api/program'
import { listMyNotifications } from '@/api/notification'
import PageHeader from '@/components/PageHeader.vue'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import ComingSoonCard from '@/components/dashboard/ComingSoonCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { humanize } from '@/utils/text'
import DashboardPanel from './dashboard/DashboardPanel.vue'
import PlatformGroup from './dashboard/PlatformGroup.vue'
import FinanceGroup from './dashboard/FinanceGroup.vue'
import ApplicantsGroup from './dashboard/ApplicantsGroup.vue'
import { countBy, openTasks, prioritySlices, statusSlices, topUniversitySlices } from './dashboard/dashboardMath'
import { useDashboardAccess } from './dashboard/useDashboardAccess'
import { useFinanceOverview } from './dashboard/useFinanceOverview'
import { useGatedLoader } from './dashboard/useGatedLoader'
import { usePlatformCounts } from './dashboard/usePlatformCounts'

const TASK_SAMPLE_SIZE = 8
const ACTIVITY_SAMPLE_SIZE = 8
const CATALOG_SAMPLE_SIZE = 200

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

// Every section is gated on the permission its API needs — a staff member only
// ever sees, and only ever fetches, what their role allows.
const access = useDashboardAccess()
const platformCounts = usePlatformCounts(access)
const { data: stats, loading, error, refresh } = useApplicantStats()
const finance = useFinanceOverview(() => access.canSeeFinance.value)
const tasks = useGatedLoader(
  [] as ReturnType<typeof openTasks>,
  () => access.canSeeTasks.value,
  async () => openTasks((await listTasks({ page: 0, size: TASK_SAMPLE_SIZE })).content),
)
const catalog = useGatedLoader(
  [] as ReturnType<typeof topUniversitySlices>,
  () => access.canSeeCatalog.value,
  async () => topUniversitySlices((await listPrograms({ page: 0, size: CATALOG_SAMPLE_SIZE })).content),
)
const activity = useGatedLoader(
  [] as Awaited<ReturnType<typeof listMyNotifications>>['content'],
  () => true,
  async () => (await listMyNotifications({ page: 0, size: ACTIVITY_SAMPLE_SIZE })).content,
)

const cardState = computed<'ok' | 'loading' | 'error'>(() =>
  error.value ? 'error' : (loading.value && !stats.value ? 'loading' : 'ok'))

const applicantChart = computed(() => statusSlices((stats.value?.recent ?? []).map(r => r.status)))
const taskChart = computed(() => prioritySlices(tasks.data.value, p => t(`tasks.priorityMap.${p}`)))
const activityChart = computed(() =>
  [...countBy(activity.data.value, a => a.type)].map(([type, value]) => ({
    label: t(`notifications.typeMap.${type}`, humanize(type)),
    value,
  })))

function refreshAll() {
  if (access.canSeeApplicants.value) refresh()
  finance.load()
  tasks.load()
  catalog.load()
  platformCounts.load()
  activity.load()
}

const comingSoon = [
  { key: 'applications', domain: 'Application' },
  { key: 'payments', domain: 'Payment' },
  { key: 'payroll', domain: 'Payroll' },
] as const

onMounted(refreshAll)
</script>

<style scoped>
.dash__row {
  margin-bottom: 16px;
}
.dash__split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 28px;
}
.dash__panel {
  border: 1px solid var(--nad-line);
  border-radius: var(--nad-radius);
}
@media (max-width: 900px) {
  .dash__split {
    grid-template-columns: 1fr;
  }
}
</style>
