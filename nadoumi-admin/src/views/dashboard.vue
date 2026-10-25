<template>
  <div class="nad-page dash">
    <PageHeader
      :title="greeting"
      :subtitle="t('dashboard.subtitle', { date: todayLabel })"
    >
      <template #actions>
        <el-button
          :loading="busy"
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
      v-if="pipeline.failed.value || finance.failed.value"
      :title="t('dashboard.loadError')"
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
      class="dash__row dash__empty"
    >
      <EmptyState
        :title="t('dashboard.noAccessTitle')"
        :description="t('dashboard.noAccessBody')"
        icon="Lock"
      />
    </el-card>

    <template v-else>
      <AttentionStrip
        v-if="attention.length"
        :items="attention"
      />

      <div
        v-if="kpis.length"
        class="dash__kpis"
      >
        <KpiCard
          v-for="k in kpis"
          :key="k.key"
          :label="k.label"
          :value="k.value"
          :tone="k.tone"
          :delta="k.delta ?? null"
          :delta-good="k.deltaGood"
          :hint="k.hint"
          :to="k.to"
        />
      </div>

      <div class="dash__grid">
        <div class="dash__main">
          <PipelineFunnel
            v-if="access.canSeeApplications.value"
            :per-status="pipeline.perStatus.value"
            :loading="pipeline.loading.value"
          />
          <LatestApplications
            v-if="access.canSeeApplications.value"
            :rows="pipeline.latest.value"
            :loading="pipeline.loading.value"
          />
          <section
            v-if="finance.overview.value"
            class="dash__card"
          >
            <div class="dash__card-head">
              <h3 class="dash__card-title">
                {{ t('dashboard.financeTitle') }}
              </h3>
              <el-button
                link
                type="primary"
                @click="router.push('/finance')"
              >
                {{ t('dashboard.openFinance') }}
              </el-button>
            </div>
            <TrendChart
              :months="finance.overview.value.months.slice(-6)"
              :currency="finance.overview.value.currency"
              :revenue-label="t('finance.revenue')"
              :expenses-label="t('finance.expenses')"
              :net-label="t('finance.net')"
            />
          </section>
        </div>

        <div class="dash__side">
          <MyTasksPanel
            v-if="access.canSeeTasks.value"
            :tasks="tasks.shown.value"
            :loading="tasks.loading.value"
            :today="today"
          />
          <ActivityPanel
            :items="activity.data.value"
            :loading="activity.loading.value"
          />
          <CatalogPanel
            v-if="catalogRows.length"
            :rows="catalogRows"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { listMyNotifications } from '@/api/notification'
import PageHeader from '@/components/PageHeader.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import AttentionStrip, { type AttentionItem } from './dashboard/AttentionStrip.vue'
import PipelineFunnel from './dashboard/PipelineFunnel.vue'
import LatestApplications from './dashboard/LatestApplications.vue'
import MyTasksPanel from './dashboard/MyTasksPanel.vue'
import ActivityPanel from './dashboard/ActivityPanel.vue'
import CatalogPanel, { type CatalogRow } from './dashboard/CatalogPanel.vue'
import { useApplicationPipeline } from './dashboard/useApplicationPipeline'
import { useDashboardAccess } from './dashboard/useDashboardAccess'
import { useFinanceGlance } from './dashboard/useFinanceGlance'
import { useGatedLoader } from './dashboard/useGatedLoader'
import { useMyTasks } from './dashboard/useMyTasks'
import { usePlatformCounts } from './dashboard/usePlatformCounts'

const ACTIVITY_SAMPLE_SIZE = 6
const MORNING_UNTIL_HOUR = 12
const AFTERNOON_UNTIL_HOUR = 18

interface Kpi {
  key: string
  label: string
  value: string
  tone?: 'good' | 'bad'
  delta?: string | null
  deltaGood?: boolean
  hint?: string
  to?: string
}

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

// Every block is gated on the permission its API needs: a staff member only sees, and only fetches, what their role allows.
const access = useDashboardAccess()
const platformCounts = usePlatformCounts(access)
const pipeline = useApplicationPipeline(access)
const finance = useFinanceGlance(access)
const now = new Date()
const today = now.toISOString().slice(0, 10)
const tasks = useMyTasks(access, () => userStore.userId, () => today)
const activity = useGatedLoader(
  [] as Awaited<ReturnType<typeof listMyNotifications>>['content'],
  () => true,
  async () => (await listMyNotifications({ page: 0, size: ACTIVITY_SAMPLE_SIZE })).content,
)

const busy = computed(() => pipeline.loading.value || finance.loading.value || tasks.loading.value)

const greeting = computed(() => {
  const hour = now.getHours()
  const part = hour < MORNING_UNTIL_HOUR ? 'morning' : hour < AFTERNOON_UNTIL_HOUR ? 'afternoon' : 'evening'
  return t(`dashboard.greeting.${part}`, { name: userStore.nickName })
})
const todayLabel = computed(() => now.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }))

const money = (v: number, currency: string) => `${currency} ${Number(v).toLocaleString('en-US', { maximumFractionDigits: 0 })}`
function delta(now: number, prev: number): string | null {
  if (!prev) return null
  const pct = Math.round(((now - prev) / Math.abs(prev)) * 100)
  return t('finance.vsLastMonth', { change: `${pct >= 0 ? '+' : ''}${pct}%` })
}

// what needs a person today, most urgent first; a block only appears for the permission behind it
const attention = computed<AttentionItem[]>(() => {
  const items: AttentionItem[] = []
  if (access.canSeePayments.value) {
    items.push({ key: 'payments', label: t('dashboard.k.paymentsToConfirm'), count: pipeline.paymentsToConfirm.value, to: '/payments', urgent: true })
  }
  if (access.canSeeApplications.value) {
    items.push({ key: 'review', label: t('dashboard.k.applicationsInReview'), count: pipeline.inReview.value, to: '/applications' })
  }
  if (access.canSeeTasks.value) {
    items.push({
      key: 'tasks', label: t('dashboard.k.myOpenTasks'), count: tasks.total.value, to: '/tasks', urgent: tasks.overdue.value > 0,
      detail: tasks.overdue.value ? t('dashboard.k.overdue', { n: tasks.overdue.value }) : undefined,
    })
  }
  const o = finance.overview.value
  if (o) {
    items.push({ key: 'bills', label: t('dashboard.k.billsToPay'), count: o.kpis.payablesCount, to: '/expenses' })
  }
  return items
})

const kpis = computed<Kpi[]>(() => {
  const list: Kpi[] = []
  if (access.canSeeStudents.value && platformCounts.counts.value.students !== null) {
    list.push({ key: 'students', label: t('dashboard.k.totalStudents'), value: String(platformCounts.counts.value.students) })
  }
  if (access.canSeeApplications.value) {
    list.push({ key: 'applications', label: t('dashboard.k.applicationsSubmitted'), value: String(pipeline.total.value), to: '/applications' })
  }
  const o = finance.overview.value
  if (o) {
    const k = o.kpis
    list.push(
      {
        key: 'revenue', label: t('finance.kRevenueMonth'), value: money(k.revenueMonth, o.currency),
        delta: delta(k.revenueMonth, k.revenueMonthPrev), deltaGood: k.revenueMonth >= k.revenueMonthPrev, to: '/finance',
      },
      {
        key: 'net', label: t('finance.kNetMonth'), value: money(k.netMonth, o.currency), tone: k.netMonth >= 0 ? 'good' : 'bad',
        delta: delta(k.netMonth, k.netMonthPrev), deltaGood: k.netMonth >= k.netMonthPrev, to: '/finance',
      },
    )
  }
  return list
})

const catalogRows = computed<CatalogRow[]>(() => {
  const c = platformCounts.counts.value
  const rows: CatalogRow[] = []
  if (access.canSeeUniversities.value) rows.push({ key: 'universities', label: t('dashboard.k.totalUniversities'), count: c.universities, to: '/universities' })
  if (access.canSeeCatalog.value) rows.push({ key: 'programmes', label: t('dashboard.k.totalProgrammes'), count: c.programmes, to: '/programs' })
  if (access.canSeeScholarships.value) rows.push({ key: 'scholarships', label: t('dashboard.k.totalScholarships'), count: c.scholarships, to: '/scholarships' })
  return rows
})

function refreshAll() {
  pipeline.load()
  finance.load()
  tasks.load()
  platformCounts.load()
  activity.load()
}

onMounted(refreshAll)
</script>

<style scoped>
.dash__row { margin-bottom: 16px; }
.dash__kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; margin-bottom: 16px; }
.dash__grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.dash__main, .dash__side { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.dash__main > :deep(.latest) { margin-bottom: 0; }
.dash__card { border: 1px solid var(--nad-line, #e5e7eb); border-radius: var(--nad-radius, 8px); padding: 16px 18px; background: var(--nad-surface, #fff); min-width: 0; }
.dash__card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.dash__card-title { margin: 0; font-size: 14px; font-weight: 650; color: var(--nad-ink, #0f172a); }
.dash__empty { border: 1px solid var(--nad-line); border-radius: var(--nad-radius); }
@media (max-width: 1100px) { .dash__grid { grid-template-columns: minmax(0, 1fr); } }
</style>
