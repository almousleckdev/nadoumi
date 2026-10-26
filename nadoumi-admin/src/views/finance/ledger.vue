<template>
  <div class="nad-page">
    <PageHeader
      :title="t('ledger.title')"
      :subtitle="t('ledger.subtitle')"
    >
      <template #actions>
        <el-button
          :icon="Download"
          :loading="exporting"
          data-test="export"
          @click="exportCsv"
        >
          {{ t('ledger.export') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('ledger.searchPlaceholder')"
        clearable
        style="width: 280px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.kind"
        :placeholder="t('ledger.type')"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option
          :label="t('ledger.in')"
          value="REVENUE"
        />
        <el-option
          :label="t('ledger.out')"
          value="EXPENSE"
        />
      </el-select>
      <el-select
        v-model="filters.currency"
        :placeholder="t('finance.currency')"
        clearable
        style="width: 120px"
        @change="reload"
      >
        <el-option
          v-for="c in CURRENCIES"
          :key="c"
          :label="c"
          :value="c"
        />
      </el-select>
      <el-date-picker
        v-model="filters.range"
        type="daterange"
        value-format="YYYY-MM-DD"
        :shortcuts="shortcuts"
        :start-placeholder="t('earnings.from')"
        :end-placeholder="t('earnings.to')"
        @change="reload"
      />
      <el-button
        :icon="Search"
        @click="reload"
      >
        {{ t('common.search') }}
      </el-button>
    </FilterBar>

    <TotalsStrip :groups="totalGroups" />

    <DataTable
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      row-key="key"
      @retry="load"
    >
      <template #cell-kind="{ row }">
        <StatusBadge
          :status="(row as Row).kind === 'REVENUE' ? 'ACTIVE' : 'CLOSED'"
          :label="(row as Row).kind === 'REVENUE' ? t('ledger.in') : t('ledger.out')"
        />
      </template>
      <template #cell-category="{ row }">
        {{ categoryLabel(row as Row) }}
      </template>
      <template #cell-amount="{ row }">
        <span
          class="ledger__amt"
          :class="(row as Row).kind === 'REVENUE' ? 'ledger__amt--in' : 'ledger__amt--out'"
        >{{ signed(row as Row) }}</span>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download, Search } from '@element-plus/icons-vue'
import { ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { exportLedger, getLedgerTotals, listLedger, type LedgerEntry, type LedgerTotal } from '@/api/finance'
import TotalsStrip, { type TotalsItem } from '@/components/finance/TotalsStrip.vue'
import { rangeShortcuts } from '@/utils/dateShortcuts'
import { saveBlob } from '@/utils/download'

const CURRENCIES = ['CNY', 'USD']

type Row = LedgerEntry & { key: string }

const { t, te } = useI18n()
const exporting = ref(false)

const shortcuts = computed(() => rangeShortcuts([
  t('finance.shortcutThisMonth'), t('finance.shortcutLastMonth'), t('finance.shortcutLast30'), t('finance.shortcutThisYear'),
]))
const totals = ref<LedgerTotal[]>([])
const amount = (v: number, currency: string) => `${currency} ${Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 })}`
const totalGroups = computed(() => totals.value.map(r => ({
  currency: r.currency,
  items: [
    { key: 'in', label: t('ledger.in'), value: amount(r.revenue, r.currency), tone: 'good' },
    { key: 'out', label: t('ledger.out'), value: amount(r.expenses, r.currency), tone: 'bad' },
    { key: 'net', label: t('finance.net'), value: amount(r.net, r.currency), tone: r.net >= 0 ? 'good' : 'bad' },
    { key: 'n', label: t('finance.totalEntries'), value: String(r.entries) },
  ] satisfies TotalsItem[],
})))

const emptyFilters = () => ({ q: '', kind: '', currency: '', range: null as [string, string] | null })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Row, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 0,
    size: 20,
    fetch: async (f, { index, size }) => {
      // the totals follow the filters, not the page, so they are asked for once per filter change
      if (index === 0) {
        getLedgerTotals({
          q: f.q || undefined, kind: f.kind || undefined, currency: f.currency || undefined, from: f.range?.[0], to: f.range?.[1],
        }).then((r) => { totals.value = r }).catch(() => { totals.value = [] })
      }
      const result = await listLedger({
        q: f.q || undefined, kind: f.kind || undefined, currency: f.currency || undefined,
        from: f.range?.[0], to: f.range?.[1], page: index, size,
      })
      return { ...result, content: result.content.map(e => ({ ...e, key: `${e.kind}-${e.id}` })) }
    },
  })

const columns = computed(() => [
  { prop: 'occurredOn', label: t('ledger.colDate'), width: 120 },
  { prop: 'kind', label: t('ledger.type'), width: 130, align: 'center' as const },
  { prop: 'category', label: t('ledger.colCategory'), minWidth: 160 },
  { prop: 'title', label: t('ledger.colTitle'), minWidth: 260 },
  { prop: 'reference', label: t('ledger.colRef'), minWidth: 150 },
  { prop: 'amount', label: t('ledger.colAmount'), width: 170, align: 'right' as const },
])

const categoryLabel = (r: Row) => (r.kind === 'REVENUE' && r.category && te(`revenue.sourceMap.${r.category}`)
  ? t(`revenue.sourceMap.${r.category}`) : (r.category ?? ''))
const signed = (r: Row) => `${r.kind === 'REVENUE' ? '+' : '-'}${r.currency} ${Number(r.amount).toLocaleString('en-US', { maximumFractionDigits: 2 })}`

async function exportCsv() {
  exporting.value = true
  try {
    const f = filters
    saveBlob(await exportLedger({
      q: f.q || undefined, kind: f.kind || undefined, currency: f.currency || undefined,
      from: f.range?.[0], to: f.range?.[1],
    }), 'nadoumi-ledger.csv')
  }
  finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ledger__amt { font-variant-numeric: tabular-nums; font-weight: 600; }
.ledger__amt--in { color: var(--el-color-success, #16a34a); }
.ledger__amt--out { color: var(--el-color-danger, #dc2626); }
</style>
