<template>
  <div class="nad-page">
    <PageHeader
      :title="t('earnings.title')"
      :subtitle="t('earnings.subtitle')"
    >
      <template #actions>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          :start-placeholder="t('earnings.from')"
          :end-placeholder="t('earnings.to')"
          :clearable="false"
          @change="load"
        />
        <el-button
          :icon="Refresh"
          :loading="loading"
          @click="load"
        >
          {{ t('common.refresh') }}
        </el-button>
      </template>
    </PageHeader>

    <ErrorState
      v-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else>
      <section
        v-for="line in summary?.byCurrency ?? []"
        :key="line.currency"
        class="earn-ccy"
      >
        <h3 class="earn-ccy__h">
          {{ line.currency }}
        </h3>
        <div class="earn-cards">
          <div class="earn-card">
            <span class="earn-card__k">{{ t('earnings.revenue') }}</span>
            <span class="earn-card__v earn-pos">{{ fmt(line.revenue, line.currency) }}</span>
          </div>
          <div class="earn-card">
            <span class="earn-card__k">{{ t('earnings.expenses') }}</span>
            <span class="earn-card__v earn-neg">{{ fmt(line.expenses, line.currency) }}</span>
          </div>
          <div class="earn-card earn-card--net">
            <span class="earn-card__k">{{ t('earnings.net') }}</span>
            <span
              class="earn-card__v"
              :class="Number(line.net) >= 0 ? 'earn-pos' : 'earn-neg'"
            >{{ fmt(line.net, line.currency) }}</span>
          </div>
        </div>
        <div class="earn-bar">
          <div
            class="earn-bar__fill"
            :style="{ width: pct(line) + '%' }"
          />
        </div>
        <p class="earn-bar__cap">
          {{ t('earnings.marginCap', { pct: pct(line) }) }}
        </p>
      </section>

      <el-empty
        v-if="!loading && !hasData"
        :description="t('earnings.emptyAll')"
      >
        <el-button
          type="primary"
          @click="router.push('/revenue')"
        >
          {{ t('revenue.new') }}
        </el-button>
        <el-button @click="router.push('/expenses')">
          {{ t('expenses.new') }}
        </el-button>
      </el-empty>

      <div
        v-if="hasData"
        class="earn-split"
      >
        <div class="earn-panel">
          <h4 class="earn-panel__h">
            {{ t('earnings.revenueBySource') }}
          </h4>
          <ul
            v-if="summary?.revenueBySource?.length"
            class="earn-list"
          >
            <li
              v-for="b in summary.revenueBySource"
              :key="b.label + b.currency"
              class="earn-list__row"
            >
              <span class="earn-list__label">{{ b.label || t('earnings.unlabelled') }}</span>
              <span class="earn-list__bar">
                <span
                  class="earn-list__bar-fill"
                  :style="{ width: barWidth(b.total, revenueMax) }"
                />
              </span>
              <span class="earn-list__val">{{ b.currency }} {{ n(b.total) }}</span>
            </li>
          </ul>
          <p
            v-else
            class="earn-panel__empty"
          >
            {{ t('earnings.empty') }}
          </p>
        </div>

        <div class="earn-panel">
          <h4 class="earn-panel__h">
            {{ t('earnings.expenseByCategory') }}
          </h4>
          <ul
            v-if="summary?.expenseByCategory?.length"
            class="earn-list"
          >
            <li
              v-for="b in summary.expenseByCategory"
              :key="b.label + b.currency"
              class="earn-list__row"
            >
              <span class="earn-list__label">{{ b.label || t('earnings.unlabelled') }}</span>
              <span class="earn-list__bar">
                <span
                  class="earn-list__bar-fill earn-list__bar-fill--exp"
                  :style="{ width: barWidth(b.total, expenseMax) }"
                />
              </span>
              <span class="earn-list__val">{{ b.currency }} {{ n(b.total) }}</span>
            </li>
          </ul>
          <p
            v-else
            class="earn-panel__empty"
          >
            {{ t('earnings.empty') }}
          </p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { getFinanceSummary, type FinanceSummary, type CurrencyLine } from '@/api/finance'

const { t } = useI18n()
const router = useRouter()

const range = ref<[string, string]>([
  `${new Date().getFullYear()}-01-01`,
  new Date().toISOString().slice(0, 10),
])
const summary = ref<FinanceSummary | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const hasData = computed(() => Boolean(
  summary.value?.byCurrency?.length
  || summary.value?.revenueBySource?.length
  || summary.value?.expenseByCategory?.length))
const revenueMax = computed(() =>
  Math.max(1, ...(summary.value?.revenueBySource ?? []).map(b => Number(b.total))))
const expenseMax = computed(() =>
  Math.max(1, ...(summary.value?.expenseByCategory ?? []).map(b => Number(b.total))))

function n(v: number | string): string {
  return Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 })
}
function fmt(v: number | string, ccy: string): string {
  return `${ccy} ${n(v)}`
}
function barWidth(v: number | string, max: number): string {
  return `${Math.max(2, (Number(v) / max) * 100)}%`
}
function pct(line: CurrencyLine): number {
  const rev = Number(line.revenue)
  if (rev <= 0) return 0
  return Math.max(0, Math.round((Number(line.net) / rev) * 100))
}

async function load() {
  loading.value = true
  error.value = null
  try {
    summary.value = await getFinanceSummary({ from: range.value[0], to: range.value[1] })
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.earn-ccy { margin-bottom: 22px; }
.earn-ccy__h { margin: 0 0 10px; font-size: 13px; letter-spacing: 0.04em; color: var(--nad-ink-soft, #64748b); }
.earn-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.earn-card {
  border: 1px solid var(--nad-line, #e5e7eb); border-radius: 10px; padding: 14px 16px;
  display: flex; flex-direction: column; gap: 6px; background: var(--nad-surface, #fff);
}
.earn-card--net { background: var(--nad-surface-2, #f8fafc); }
.earn-card__k { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.earn-card__v { font-size: 20px; font-weight: 680; font-variant-numeric: tabular-nums; }
.earn-pos { color: var(--el-color-success, #16a34a); }
.earn-neg { color: var(--el-color-danger, #dc2626); }
.earn-bar { height: 6px; border-radius: 3px; background: var(--nad-line, #e5e7eb); margin-top: 10px; overflow: hidden; }
.earn-bar__fill { height: 100%; background: var(--el-color-success, #16a34a); }
.earn-bar__cap { margin: 4px 0 0; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
.earn-split { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 8px; }
.earn-panel { border: 1px solid var(--nad-line, #e5e7eb); border-radius: 10px; padding: 16px; }
.earn-panel__h { margin: 0 0 12px; font-size: 13px; }
.earn-panel__empty { color: var(--nad-ink-faint, #9ca3af); font-size: 13px; margin: 0; }
.earn-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.earn-list__row { display: grid; grid-template-columns: 140px 1fr auto; align-items: center; gap: 10px; font-size: 13px; }
.earn-list__bar { height: 8px; background: var(--nad-line, #e5e7eb); border-radius: 4px; overflow: hidden; }
.earn-list__bar-fill { display: block; height: 100%; background: var(--nad-brand-500, #6366f1); }
.earn-list__bar-fill--exp { background: var(--el-color-warning, #d97706); }
.earn-list__val { font-variant-numeric: tabular-nums; color: var(--nad-ink-soft, #64748b); }
@media (max-width: 860px) {
  .earn-cards { grid-template-columns: 1fr; }
  .earn-split { grid-template-columns: 1fr; }
}
</style>
