<template>
  <div class="nad-page">
    <PageHeader
      :title="t('expenses.title')"
      :subtitle="t('expenses.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:expense:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('expenses.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="Boolean(filters.q || filters.status || filters.categoryId)"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('expenses.searchPlaceholder')"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('expenses.status')"
        clearable
        style="width: 150px"
        @change="reload"
      >
        <el-option
          v-for="s in EXPENSE_STATUSES"
          :key="s"
          :label="t(`expenses.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="filters.categoryId"
        :placeholder="t('expenses.category')"
        clearable
        filterable
        style="width: 180px"
        @change="reload"
      >
        <el-option
          v-for="c in categories"
          :key="c.id"
          :label="c.name"
          :value="c.id"
        />
      </el-select>
      <el-button
        :icon="Search"
        @click="reload"
      >
        {{ t('common.search') }}
      </el-button>
    </FilterBar>

    <DataTable
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      row-key="id"
      :empty-title="t('expenses.emptyTitle')"
      @retry="load"
    >
      <template #cell-title="{ row }">
        <div class="ex-title">
          <span class="ex-title__main">{{ (row as Expense).title }}</span>
          <span class="ex-title__sub">
            {{ (row as Expense).categoryName || t('expenses.uncategorised') }}
            <template v-if="(row as Expense).receiptNo"> · {{ (row as Expense).receiptNo }}</template>
          </span>
        </div>
      </template>
      <template #cell-amount="{ row }">
        {{ money((row as Expense).amount) }}
      </template>
      <template #cell-status="{ row }">
        <StatusBadge
          :status="tone((row as Expense).status)"
          :label="t(`expenses.statusMap.${(row as Expense).status}`)"
        />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="openEdit(row as Expense)"
        >
          {{ isEditable(row as Expense) ? t('common.edit') : t('common.view') }}
        </el-button>
        <el-button
          v-if="['APPROVED', 'PAID'].includes((row as Expense).status)"
          link
          @click="openReceipt(row as Expense)"
        >
          {{ t('expenses.receipt') }}
        </el-button>
        <el-dropdown
          v-if="nextStates(row as Expense).length"
          trigger="click"
          @command="(s: ExpenseStatus) => move(row as Expense, s)"
        >
          <el-button
            link
            type="primary"
          >
            {{ t('expenses.changeStatus') }}<el-icon class="el-icon--right">
              <ArrowDown />
            </el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="s in nextStates(row as Expense)"
                :key="s"
                :command="s"
              >
                {{ t(`expenses.statusMap.${s}`) }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button
          v-if="userStore.hasPerm('nad:expense:remove') && ['DRAFT', 'REJECTED'].includes((row as Expense).status)"
          link
          type="danger"
          @click="doDelete(row as Expense)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="filters.page"
      v-model:size="filters.size"
      :total="total"
      @change="load"
    />

    <ExpenseDrawer
      v-model="drawerOpen"
      :expense-id="editingId"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Search, ArrowDown } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import ExpenseDrawer from './ExpenseDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { dualMoney } from '@/utils/money'
import {
  listExpenses, deleteExpense, changeExpenseStatus, listExpenseCategories, expenseReceiptUrl,
  EXPENSE_STATUSES, EXPENSE_TRANSITIONS,
  type Expense, type ExpenseStatus, type ExpenseCategory,
} from '@/api/finance'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const canApprove = computed(() => userStore.hasPerm('nad:expense:approve'))

const rows = ref<Expense[]>([])
const categories = ref<ExpenseCategory[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const filters = reactive({ q: '', status: '', categoryId: undefined as number | undefined, page: 1, size: 20 })

const columns = computed(() => [
  { prop: 'title', label: t('expenses.expenseTitle'), minWidth: 240 },
  { prop: 'amount', label: t('expenses.amount'), width: 130, align: 'right' as const },
  { prop: 'spentOn', label: t('expenses.spentOn'), width: 120 },
  { prop: 'vendor', label: t('expenses.vendor'), minWidth: 140 },
  { prop: 'status', label: t('expenses.status'), width: 120, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 260, align: 'right' as const },
])

function money(amount: number): string {
  return dualMoney(amount, null)
}
function tone(s: ExpenseStatus): string {
  return s === 'PAID' ? 'ACTIVE' : s === 'APPROVED' ? 'IN_REVIEW'
    : s === 'REJECTED' ? 'FAILED' : s === 'SUBMITTED' ? 'PENDING' : 'DRAFT'
}
function isEditable(e: Expense) {
  return ['DRAFT', 'SUBMITTED', 'REJECTED'].includes(e.status) && userStore.hasPerm('nad:expense:edit')
}
/** Forward transitions this user is allowed to trigger. */
function nextStates(e: Expense): ExpenseStatus[] {
  if (!userStore.hasPerm('nad:expense:edit')) return []
  const all = EXPENSE_TRANSITIONS[e.status] ?? []
  const approvalMoves: ExpenseStatus[] = ['APPROVED', 'PAID', 'REJECTED']
  return all.filter(s => !approvalMoves.includes(s) || canApprove.value)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listExpenses({
      q: filters.q || undefined,
      status: filters.status || undefined,
      categoryId: filters.categoryId,
      page: filters.page - 1,
      size: filters.size,
    })
    rows.value = res.content
    total.value = res.totalElements
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}
function reload() { filters.page = 1; load() }
function clearFilters() {
  filters.q = ''; filters.status = ''; filters.categoryId = undefined
  reload()
}
function openCreate() { editingId.value = undefined; drawerOpen.value = true }
function openEdit(e: Expense) { editingId.value = e.id; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load() }
function openReceipt(e: Expense) { window.open(expenseReceiptUrl(e.id), '_blank', 'noopener') }

async function move(e: Expense, to: ExpenseStatus) {
  if (!(await confirm({
    title: t('expenses.confirmStatusTitle'),
    message: t('expenses.confirmStatus', { title: e.title, status: t(`expenses.statusMap.${to}`) }),
  }))) return
  await changeExpenseStatus(e.id, to)
  ElMessage.success(t('common.saved'))
  load()
}

async function doDelete(e: Expense) {
  if (!(await confirm({
    title: t('expenses.deleteTitle'),
    message: t('expenses.deleteConfirm', { title: e.title }),
    tone: 'danger',
  }))) return
  await deleteExpense(e.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(async () => {
  categories.value = await listExpenseCategories().catch(() => [])
  load()
})
</script>

<style scoped>
.ex-title__main { display: block; font-weight: 600; }
.ex-title__sub { display: block; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
</style>
