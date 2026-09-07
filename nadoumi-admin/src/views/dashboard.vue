<template>
  <div class="nad-page dash">
    <PageHeader
      :title="t('dashboard.title')"
      :subtitle="t('dashboard.subtitle')"
    >
      <template #actions>
        <el-button
          :loading="loading || financeLoading"
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

    <!-- Nothing this account can see yet -->
    <el-card
      v-if="nothingVisible"
      shadow="never"
      class="dash__panel dash__row"
    >
      <EmptyState
        :title="t('dashboard.noAccessTitle')"
        :description="t('dashboard.noAccessBody')"
        icon="Lock"
      />
    </el-card>

    <!-- Platform totals -->
    <DashboardGroup
      v-if="showPlatform"
      :title="t('dashboard.groups.platform')"
      :subtitle="t('dashboard.liveData')"
    >
      <StatCard
        v-if="canSeeStudents"
        :label="t('dashboard.k.totalStudents')"
        :value="counts.students"
        :state="counts.students === null ? 'loading' : 'ok'"
      />
      <StatCard
        v-if="canSeeApplicants"
        :label="t('dashboard.k.totalApplicants')"
        :value="stats?.total ?? null"
        :state="cardState"
      />
      <StatCard
        v-if="canSeeUniversities"
        :label="t('dashboard.k.totalUniversities')"
        :value="counts.universities"
        :state="counts.universities === null ? 'loading' : 'ok'"
      />
      <StatCard
        v-if="canSeeCatalog"
        :label="t('dashboard.k.totalProgrammes')"
        :value="counts.programmes"
        :state="counts.programmes === null ? 'loading' : 'ok'"
      />
      <StatCard
        v-if="canSeeScholarships"
        :label="t('dashboard.k.totalScholarships')"
        :value="counts.scholarships"
        :state="counts.scholarships === null ? 'loading' : 'ok'"
      />
    </DashboardGroup>

    <!-- Finance — net earnings, gated on nad:finance:view -->
    <DashboardGroup
      v-if="canSeeFinance"
      :title="t('dashboard.groups.finance')"
      :subtitle="t('dashboard.thisYear')"
    >
      <StatCard
        :label="t('dashboard.k.revenueToday')"
        :value="revToday.text"
        :hint="t('dashboard.today')"
        :state="financeState"
      />
      <StatCard
        :label="t('dashboard.k.revenueTotal')"
        :value="revAll.text"
        :hint="t('dashboard.allTime')"
        :state="financeState"
      />
      <StatCard
        :label="t('dashboard.k.revenue')"
        :value="fin.revenue"
        :hint="fin.currency"
        :state="financeState"
      />
      <StatCard
        :label="t('dashboard.k.expenses')"
        :value="fin.expenses"
        :hint="fin.currency"
        :state="financeState"
      />
      <StatCard
        :label="t('dashboard.k.netEarnings')"
        :value="fin.net"
        :hint="fin.currency"
        :state="financeState"
      />
      <StatCard
        :label="t('dashboard.k.netMargin')"
        :value="fin.margin"
        :state="financeState"
      />
    </DashboardGroup>

    <!-- Applicants — real data from /api/staff/applicants -->
    <DashboardGroup
      v-if="canSeeApplicants"
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

    <!-- Popular universities (real: programmes per university) + recent applicants -->
    <div
      v-if="canSeeCatalog || canSeeApplicants"
      class="dash__split"
    >
      <el-card
        v-if="canSeeCatalog"
        shadow="never"
        class="dash__panel"
      >
        <template #header>
          <span class="dash__panel-title">{{ t('dashboard.popularUniversities') }}</span>
        </template>
        <DonutChart
          v-if="uniChart.length"
          :slices="uniChart"
          :caption="t('dashboard.k.totalProgrammes')"
        />
        <EmptyState
          v-else
          :title="catalogLoading ? t('common.loading') : t('dashboard.noProgrammes')"
          icon="Notebook"
        />
      </el-card>
      <el-card
        v-if="canSeeApplicants"
        shadow="never"
        class="dash__panel"
      >
        <template #header>
          <span class="dash__panel-title">{{ t('dashboard.recentApplicants') }}</span>
          <el-button
            link
            type="primary"
            @click="router.push('/applicants')"
          >
            {{ t('dashboard.viewAll') }}
          </el-button>
        </template>
        <DonutChart
          v-if="applicantChart.length"
          :slices="applicantChart"
          :caption="t('dashboard.byStatus')"
        />
        <EmptyState
          v-else
          :title="loading && !stats ? t('common.loading') : t('dashboard.noApplicants')"
          icon="User"
        />
      </el-card>
    </div>

    <!-- My open tasks (any staff) + recent activity -->
    <div
      v-if="!nothingVisible"
      class="dash__split"
    >
      <el-card
        v-if="canSeeTasks"
        shadow="never"
        class="dash__panel"
      >
        <template #header>
          <span class="dash__panel-title">{{ t('dashboard.myTasks') }}</span>
          <el-button
            link
            type="primary"
            @click="router.push('/tasks')"
          >
            {{ t('dashboard.viewAll') }}
          </el-button>
        </template>
        <DonutChart
          v-if="taskChart.length"
          :slices="taskChart"
          :caption="t('dashboard.byPriority')"
        />
        <EmptyState
          v-else
          :title="tasksLoading ? t('common.loading') : t('dashboard.noOpenTasks')"
          icon="Select"
        />
      </el-card>
      <el-card
        shadow="never"
        class="dash__panel"
      >
        <template #header>
          <span class="dash__panel-title">{{ t('dashboard.recentActivity') }}</span>
          <el-button
            link
            type="primary"
            @click="router.push('/notifications')"
          >
            {{ t('dashboard.viewAll') }}
          </el-button>
        </template>
        <DonutChart
          v-if="activityChart.length"
          :slices="activityChart"
          :caption="t('dashboard.byType')"
        />
        <EmptyState
          v-else
          :title="activityLoading ? t('common.loading') : t('dashboard.noActivity')"
          icon="Bell"
        />
      </el-card>
    </div>

    <!-- Roadmap — only the platform owner needs to see what isn't built yet -->
    <DashboardGroup
      v-if="isOwner"
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
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useApplicantStats } from '@/composables/useApplicantStats'
import { getFinanceSummary, type FinanceSummary } from '@/api/finance'
import { listTasks, type Task } from '@/api/hr'
import { listPrograms } from '@/api/program'
import { listUniversities } from '@/api/university'
import { listScholarships } from '@/api/scholarship'
import { listUsers } from '@/api/system'
import { listMyNotifications, type NotificationView } from '@/api/notification'
import PageHeader from '@/components/PageHeader.vue'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import ComingSoonCard from '@/components/dashboard/ComingSoonCard.vue'
import DonutChart from '@/components/dashboard/DonutChart.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

// Every section is gated on the permission its API needs — a staff member only
// ever sees, and only ever fetches, what their role allows.
const canSeeApplicants = computed(() => userStore.hasPerm('nad:applicant:list'))
const canSeeFinance = computed(() => userStore.hasPerm('nad:finance:view'))
const canSeeTasks = computed(() => userStore.hasPerm('nad:task:list'))
const canSeeCatalog = computed(() => userStore.hasPerm('nad:program:list'))
const canSeeUniversities = computed(() => userStore.hasPerm('nad:university:list'))
const canSeeScholarships = computed(() => userStore.hasPerm('nad:scholarship:list'))
const canSeeStudents = computed(() => userStore.hasPerm('system:user:list'))
const isOwner = computed(() => userStore.hasPerm('*:*:*'))
const nothingVisible = computed(() =>
  !canSeeApplicants.value && !canSeeFinance.value && !canSeeTasks.value && !canSeeCatalog.value
  && !canSeeUniversities.value && !canSeeScholarships.value && !canSeeStudents.value)

/* ---- platform totals (each card fetches only if its list perm is held) ---- */
const counts = ref<Record<string, number | null>>({
  students: null, applicants: null, universities: null, programmes: null, scholarships: null,
})
const showPlatform = computed(() => !nothingVisible.value && (
  canSeeStudents.value || canSeeApplicants.value || canSeeUniversities.value
  || canSeeCatalog.value || canSeeScholarships.value))
async function loadCounts() {
  const jobs: Promise<void>[] = []
  const grab = async (key: string, p: Promise<{ total?: number, totalElements?: number }>) => {
    try { const r = await p; counts.value[key] = r.total ?? r.totalElements ?? 0 }
    catch { counts.value[key] = null }
  }
  if (canSeeStudents.value) jobs.push(grab('students', listUsers({ userType: '10', pageNum: 1, pageSize: 1 })))
  if (canSeeUniversities.value) jobs.push(grab('universities', listUniversities({ page: 0, size: 1 })))
  if (canSeeCatalog.value) jobs.push(grab('programmes', listPrograms({ page: 0, size: 1 })))
  if (canSeeScholarships.value) jobs.push(grab('scholarships', listScholarships({ page: 0, size: 1 })))
  await Promise.all(jobs)
}

/* ---- applicants ---- */
const { data: stats, loading, error, refresh } = useApplicantStats()
const cardState = computed<'ok' | 'loading' | 'error'>(() =>
  error.value ? 'error' : (loading.value && !stats.value ? 'loading' : 'ok'))

/* ---- finance ---- */
const finance = ref<FinanceSummary | null>(null)
const financeToday = ref<FinanceSummary | null>(null)
const financeAll = ref<FinanceSummary | null>(null)
const financeLoading = ref(false)
const financeState = computed<'ok' | 'loading' | 'error'>(() =>
  financeLoading.value && !finance.value ? 'loading' : 'ok')

function sumRevenue(s: FinanceSummary | null): { text: string, ccy: string } {
  const lines = s?.byCurrency ?? []
  if (!lines.length) return { text: 'CNY 0', ccy: 'CNY' }
  // one card can't add across currencies — show the largest revenue line
  const top = [...lines].sort((a, b) => Number(b.revenue) - Number(a.revenue))[0]
  const ccy = top.currency ?? 'CNY'
  const v = Number(top.revenue ?? 0)
  return { text: `${ccy} ${v.toLocaleString('en-US', { maximumFractionDigits: 0 })}`, ccy }
}
const revToday = computed(() => sumRevenue(financeToday.value))
const revAll = computed(() => sumRevenue(financeAll.value))
const fin = computed(() => {
  const lines = finance.value?.byCurrency ?? []
  const top = lines.length
    ? [...lines].sort((a, b) =>
        (Number(b.revenue) + Number(b.expenses)) - (Number(a.revenue) + Number(a.expenses)))[0]
    : null
  const ccy = top?.currency ?? 'CNY'
  const rev = Number(top?.revenue ?? 0)
  const exp = Number(top?.expenses ?? 0)
  const net = Number(top?.net ?? 0)
  const money = (v: number) => `${ccy} ${v.toLocaleString('en-US', { maximumFractionDigits: 0 })}`
  return {
    currency: ccy,
    revenue: money(rev),
    expenses: money(exp),
    net: money(net),
    margin: rev > 0 ? `${Math.round((net / rev) * 100)}%` : '0%',
  }
})
async function loadFinance() {
  if (!canSeeFinance.value) return
  financeLoading.value = true
  const today = new Date().toISOString().slice(0, 10)
  try {
    const [ytd, day, all] = await Promise.all([
      getFinanceSummary({ from: `${new Date().getFullYear()}-01-01` }),
      getFinanceSummary({ from: today, to: today }),
      getFinanceSummary({}),
    ])
    finance.value = ytd
    financeToday.value = day
    financeAll.value = all
  }
  catch { finance.value = null }
  finally { financeLoading.value = false }
}

/* ---- my open tasks ---- */
const myTasks = ref<Task[]>([])
const tasksLoading = ref(false)
async function loadTasks() {
  if (!canSeeTasks.value) return
  tasksLoading.value = true
  try {
    const res = await listTasks({ page: 0, size: 8 })
    myTasks.value = res.content.filter(tk => tk.status === 'PENDING' || tk.status === 'IN_PROGRESS')
  }
  catch { myTasks.value = [] }
  finally { tasksLoading.value = false }
}

/* ---- popular universities (programmes per university) ---- */
const topUniversities = ref<{ name: string, count: number }[]>([])
const catalogLoading = ref(false)
async function loadCatalog() {
  if (!canSeeCatalog.value) return
  catalogLoading.value = true
  try {
    const res = await listPrograms({ page: 0, size: 200 })
    const byUni = new Map<string, number>()
    for (const p of res.content) {
      const name = p.universityName || 'Unknown'
      byUni.set(name, (byUni.get(name) ?? 0) + 1)
    }
    topUniversities.value = [...byUni.entries()]
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 8)
  }
  catch { topUniversities.value = [] }
  finally { catalogLoading.value = false }
}

/* ---- chart data ---- */
function titleCase(s: string): string {
  return s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ')
}
const uniChart = computed(() => topUniversities.value.map(u => ({ label: u.name, value: u.count })))
const applicantChart = computed(() => {
  const by = new Map<string, number>()
  for (const r of stats.value?.recent ?? []) by.set(r.status, (by.get(r.status) ?? 0) + 1)
  return [...by.entries()].map(([label, value]) => ({ label: titleCase(label), value }))
})
const taskChart = computed(() => {
  const order = ['HIGH', 'MEDIUM', 'LOW']
  const by = new Map<string, number>()
  for (const tk of myTasks.value) by.set(tk.priority, (by.get(tk.priority) ?? 0) + 1)
  return order.filter(p => by.has(p)).map(p => ({ label: t(`tasks.priorityMap.${p}`), value: by.get(p) as number }))
})
const activityChart = computed(() => {
  const by = new Map<string, number>()
  for (const a of activity.value) by.set(a.type, (by.get(a.type) ?? 0) + 1)
  return [...by.entries()].map(([type, value]) => ({ label: t(`notifications.typeMap.${type}`, titleCase(type)), value }))
})

/* ---- recent activity (the caller's own notification feed) ---- */
const activity = ref<NotificationView[]>([])
const activityLoading = ref(false)
async function loadActivity() {
  activityLoading.value = true
  try {
    const res = await listMyNotifications({ page: 0, size: 8 })
    activity.value = res.content
  }
  catch { activity.value = [] }
  finally { activityLoading.value = false }
}
function refreshAll() {
  if (canSeeApplicants.value) refresh()
  loadFinance()
  loadTasks()
  loadCatalog()
  loadCounts()
  loadActivity()
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
.dash__panel-title {
  font-weight: 650;
  font-size: 14px;
  color: var(--nad-ink);
}
.dash__panel :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
@media (max-width: 900px) {
  .dash__split {
    grid-template-columns: 1fr;
  }
}
</style>
