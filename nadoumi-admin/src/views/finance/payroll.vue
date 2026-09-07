<template>
  <div class="nad-page">
    <PageHeader
      :title="t('payroll.title')"
      :subtitle="t('payroll.subtitle')"
    >
      <template #actions>
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
      <div class="pr-cards">
        <div
          v-for="line in summary?.byCurrency ?? []"
          :key="line.currency"
          class="pr-card"
        >
          <span class="pr-card__ccy">{{ line.currency }}</span>
          <div class="pr-card__row">
            <span class="pr-card__k">{{ t('payroll.monthly') }}</span>
            <span class="pr-card__v">{{ money(line.monthly, line.currency) }}</span>
          </div>
          <div class="pr-card__row">
            <span class="pr-card__k">{{ t('payroll.annual') }}</span>
            <span class="pr-card__v">{{ money(line.annual, line.currency) }}</span>
          </div>
          <span class="pr-card__hc">{{ t('payroll.headcountN', { n: line.headcount }) }}</span>
        </div>
      </div>

      <DataTable
        :rows="summary?.rows ?? []"
        :columns="columns"
        :loading="loading"
        row-key="employeeId"
        :empty-title="t('payroll.empty')"
      >
        <template #cell-salaryAmount="{ row }">
          {{ money((row as PayrollRow).salaryAmount, (row as PayrollRow).salaryCurrency) }}
          <span class="pr-freq">/ {{ t(`employees.freqMap.${(row as PayrollRow).payFrequency}`, (row as PayrollRow).payFrequency) }}</span>
        </template>
        <template #cell-monthlyEquivalent="{ row }">
          {{ money((row as PayrollRow).monthlyEquivalent, (row as PayrollRow).salaryCurrency) }}
        </template>
        <template #cell-employmentStatus="{ row }">
          <StatusBadge
            :status="tone((row as PayrollRow).employmentStatus)"
            :label="t(`employees.statusMap.${(row as PayrollRow).employmentStatus}`, (row as PayrollRow).employmentStatus)"
          />
        </template>
      </DataTable>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { getPayroll, type PayrollSummary, type PayrollRow } from '@/api/hr'

const { t } = useI18n()

const summary = ref<PayrollSummary | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const columns = computed(() => [
  { prop: 'name', label: t('payroll.employee'), minWidth: 180 },
  { prop: 'deptName', label: t('employees.dept'), minWidth: 140 },
  { prop: 'positionTitle', label: t('employees.position'), minWidth: 160 },
  { prop: 'salaryAmount', label: t('payroll.salary'), minWidth: 180 },
  { prop: 'monthlyEquivalent', label: t('payroll.monthlyEquivalent'), width: 160, align: 'right' as const },
  { prop: 'employmentStatus', label: t('employees.status'), width: 120, align: 'center' as const },
])

function money(v: number | string, ccy: string): string {
  return `${ccy} ${Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 })}`
}
function tone(s: string): string {
  return s === 'ACTIVE' ? 'ACTIVE' : s === 'TERMINATED' || s === 'SUSPENDED' ? 'FAILED'
    : s === 'ON_LEAVE' ? 'PENDING' : 'DRAFT'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    summary.value = await getPayroll()
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
.pr-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-bottom: 18px; }
.pr-card {
  border: 1px solid var(--nad-line, #e5e7eb); border-radius: 10px; padding: 14px 16px;
  display: flex; flex-direction: column; gap: 6px; background: var(--nad-surface, #fff);
}
.pr-card__ccy { font-size: 12px; letter-spacing: 0.06em; color: var(--nad-ink-soft, #64748b); }
.pr-card__row { display: flex; justify-content: space-between; align-items: baseline; }
.pr-card__k { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.pr-card__v { font-weight: 650; font-variant-numeric: tabular-nums; }
.pr-card__hc { font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
.pr-freq { color: var(--nad-ink-soft, #64748b); font-size: 12px; }
</style>
