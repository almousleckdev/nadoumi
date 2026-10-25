<template>
  <div class="nad-page">
    <PageHeader
      :title="t('finance.title')"
      :subtitle="t('finance.subtitle')"
    >
      <template #actions>
        <el-select
          v-model="currency"
          style="width: 110px"
          :aria-label="t('finance.currency')"
          data-test="currency"
          @change="load"
        >
          <el-option
            v-for="c in overview?.currencies ?? [currency]"
            :key="c"
            :label="c"
            :value="c"
          />
        </el-select>
        <el-button
          :icon="Refresh"
          :loading="loading"
          @click="load"
        >
          {{ t('common.refresh') }}
        </el-button>
        <el-button
          :icon="Download"
          :loading="exporting"
          data-test="export"
          @click="exportYear"
        >
          {{ t('finance.exportYear') }}
        </el-button>
      </template>
    </PageHeader>

    <ErrorState
      v-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="overview">
      <div class="fin-kpis">
        <KpiCard
          :label="t('finance.kRevenueMonth')"
          :value="money(k.revenueMonth)"
          :delta="delta(k.revenueMonth, k.revenueMonthPrev)"
          :delta-good="k.revenueMonth >= k.revenueMonthPrev"
        />
        <KpiCard
          :label="t('finance.kExpensesMonth')"
          :value="money(k.expensesMonth)"
          :delta="delta(k.expensesMonth, k.expensesMonthPrev)"
          :delta-good="k.expensesMonth <= k.expensesMonthPrev"
        />
        <KpiCard
          :label="t('finance.kNetMonth')"
          :value="money(k.netMonth)"
          :tone="k.netMonth >= 0 ? 'good' : 'bad'"
          :delta="delta(k.netMonth, k.netMonthPrev)"
          :delta-good="k.netMonth >= k.netMonthPrev"
        />
        <KpiCard
          :label="t('finance.kRevenueToday')"
          :value="money(k.revenueToday)"
          :hint="overview.asOf"
        />
        <KpiCard
          :label="t('finance.kRevenueYtd')"
          :value="money(k.revenueYtd)"
        />
        <KpiCard
          :label="t('finance.kExpensesYtd')"
          :value="money(k.expensesYtd)"
        />
        <KpiCard
          :label="t('finance.kNetYtd')"
          :value="money(k.netYtd)"
          :tone="k.netYtd >= 0 ? 'good' : 'bad'"
          :hint="t('finance.margin', { pct: margin })"
        />
        <KpiCard
          :label="t('finance.kPayables')"
          :value="money(k.payables)"
          :hint="t('finance.payablesHint', { n: k.payablesCount })"
          to="/expenses"
          data-test="payables"
        />
      </div>

      <div
        v-if="k.receivablesCount > 0"
        class="fin-attention"
        data-test="receivables"
        role="link"
        tabindex="0"
        @click="router.push('/payments')"
        @keyup.enter="router.push('/payments')"
      >
        <strong>{{ money(k.receivables) }}</strong>
        <span>{{ t('finance.receivablesHint', { n: k.receivablesCount }) }}</span>
        <el-button
          link
          type="primary"
        >
          {{ t('finance.reviewPayments') }}
        </el-button>
      </div>

      <section class="fin-card">
        <h3 class="fin-card__h">
          {{ t('finance.trendTitle') }}
        </h3>
        <TrendChart
          :months="overview.months"
          :currency="overview.currency"
          :revenue-label="t('finance.revenue')"
          :expenses-label="t('finance.expenses')"
          :net-label="t('finance.net')"
        />
      </section>

      <div class="fin-split">
        <section class="fin-card">
          <h3 class="fin-card__h">
            {{ t('finance.sourcesTitle') }}
          </h3>
          <BreakdownList
            :rows="sources"
            :empty="t('finance.empty')"
            :currency="overview.currency"
          />
        </section>
        <section class="fin-card">
          <h3 class="fin-card__h">
            {{ t('finance.categoriesTitle') }}
          </h3>
          <BreakdownList
            :rows="categories"
            :empty="t('finance.empty')"
            :currency="overview.currency"
            tone="muted"
          />
        </section>
      </div>

      <div class="fin-actions">
        <el-button
          type="primary"
          @click="router.push('/finance/ledger')"
        >
          {{ t('finance.openLedger') }}
        </el-button>
        <el-button @click="router.push('/revenue')">
          {{ t('finance.recordRevenue') }}
        </el-button>
        <el-button @click="router.push('/expenses')">
          {{ t('finance.newExpense') }}
        </el-button>
        <el-button @click="router.push('/payroll')">
          {{ t('nav.items.payroll') }}
        </el-button>
      </div>
    </template>

    <el-skeleton
      v-else
      :rows="8"
      animated
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Download, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { exportLedger, getFinanceOverview, type FinanceOverview } from '@/api/finance'
import { saveBlob } from '@/utils/download'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import BreakdownList from './overview/BreakdownList.vue'

const { t, te } = useI18n()
const router = useRouter()

const currency = ref('CNY')
const overview = ref<FinanceOverview | null>(null)
const loading = ref(false)
const exporting = ref(false)
const error = ref<string | null>(null)

const k = computed(() => overview.value!.kpis)
const margin = computed(() => (k.value.revenueYtd > 0 ? Math.round((k.value.netYtd / k.value.revenueYtd) * 100) : 0))

const label = (group: string, raw: string) => (te(`${group}.${raw}`) ? t(`${group}.${raw}`) : raw)
const sources = computed(() => (overview.value?.revenueBySource ?? []).map(b => ({ ...b, label: label('revenue.sourceMap', b.label) })))
const categories = computed(() => overview.value?.expenseByCategory ?? [])

const money = (v: number) => `${currency.value} ${Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 })}`

/** "+12% vs last month", or null when last month had nothing to compare with. */
function delta(now: number, prev: number): string | null {
  if (!prev) return null
  const pct = Math.round(((now - prev) / Math.abs(prev)) * 100)
  return t('finance.vsLastMonth', { change: `${pct >= 0 ? '+' : ''}${pct}%` })
}

async function load() {
  loading.value = true
  error.value = null
  try {
    overview.value = await getFinanceOverview(currency.value)
    currency.value = overview.value.currency
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

async function exportYear() {
  exporting.value = true
  try {
    const year = new Date().getFullYear()
    saveBlob(await exportLedger({ currency: currency.value, from: `${year}-01-01` }), `nadoumi-ledger-${year}.csv`)
  }
  finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.fin-kpis { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.fin-attention {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px 14px; padding: 12px 16px; margin-bottom: 16px;
  border: 1px solid var(--el-color-warning-light-5, #fcd34d); background: var(--el-color-warning-light-9, #fffbeb);
  border-radius: 12px; cursor: pointer;
}
.fin-card { border: 1px solid var(--nad-line, #e5e7eb); border-radius: 12px; padding: 16px 18px; margin-bottom: 16px; background: var(--nad-surface, #fff); min-width: 0; }
.fin-card__h { margin: 0 0 12px; font-size: 14px; font-weight: 650; color: var(--nad-ink, #0f172a); }
.fin-split { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.fin-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 4px 0 24px; }
@media (max-width: 1100px) { .fin-kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) { .fin-kpis, .fin-split { grid-template-columns: 1fr; } }
</style>
