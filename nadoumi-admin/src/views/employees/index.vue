<template>
  <div class="nad-page">
    <PageHeader
      :title="t('employees.title')"
      :subtitle="t('employees.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:employee:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('employees.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="Boolean(filters.q || filters.employmentStatus)"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('employees.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.employmentStatus"
        :placeholder="t('employees.status')"
        clearable
        style="width: 170px"
        @change="reload"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="t(`employees.statusMap.${s}`)"
          :value="s"
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
      :empty-title="t('employees.emptyTitle')"
      @retry="load"
    >
      <template #cell-nickName="{ row }">
        <div class="emp-name">
          <span class="emp-name__main">{{ (row as Employee).nickName }}</span>
          <span class="emp-name__sub">{{ (row as Employee).userName }} · {{ (row as Employee).employeeNo || '' }}</span>
        </div>
      </template>
      <template #cell-employmentStatus="{ row }">
        <StatusBadge
          :status="empTone((row as Employee).employmentStatus)"
          :label="t(`employees.statusMap.${(row as Employee).employmentStatus}`)"
        />
      </template>
      <template #cell-salary="{ row }">
        <span v-if="(row as Employee).compensationVisible && (row as Employee).salaryAmount != null">
          {{ money((row as Employee).salaryAmount, (row as Employee).salaryCurrency) }}
          <span class="emp-freq">/ {{ t(`employees.freqMap.${(row as Employee).payFrequency}`) }}</span>
        </span>
        <span
          v-else
          class="nad-muted"
        >{{ (row as Employee).compensationVisible ? '' : t('employees.hidden') }}</span>
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="openEdit((row as Employee).id)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:employee:remove')"
          link
          type="danger"
          @click="doDelete(row as Employee)"
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

    <EmployeeDrawer
      v-model="drawerOpen"
      :employee-id="editingId"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import EmployeeDrawer from './EmployeeDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listEmployees, deleteEmployee, type Employee } from '@/api/hr'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const STATUSES = ['PROBATION', 'ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED']

const rows = ref<Employee[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const filters = reactive({ q: '', employmentStatus: '', page: 1, size: 20 })

const columns = computed(() => [
  { prop: 'nickName', label: t('employees.name'), minWidth: 200 },
  { prop: 'positionTitle', label: t('employees.position'), minWidth: 150 },
  { prop: 'deptName', label: t('employees.dept'), minWidth: 130 },
  { prop: 'employmentType', label: t('employees.type'), width: 110 },
  { prop: 'startDate', label: t('employees.startDate'), width: 120 },
  { prop: 'salary', label: t('employees.salary'), width: 160 },
  { prop: 'employmentStatus', label: t('employees.status'), width: 120, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 140, align: 'right' as const },
])

function empTone(s: string): string {
  return s === 'ACTIVE' ? 'ACTIVE' : s === 'TERMINATED' || s === 'SUSPENDED' ? 'FAILED'
    : s === 'ON_LEAVE' ? 'PENDING' : 'DRAFT'
}
function money(amount: number | null, ccy: string | null): string {
  return `${ccy || ''} ${Number(amount ?? 0).toLocaleString()}`.trim()
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listEmployees({
      q: filters.q || undefined,
      employmentStatus: filters.employmentStatus || undefined,
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
function clearFilters() { filters.q = ''; filters.employmentStatus = ''; reload() }
function openCreate() { editingId.value = undefined; drawerOpen.value = true }
function openEdit(id: number) { editingId.value = id; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load() }

async function doDelete(row: Employee) {
  if (!(await confirm({
    title: t('employees.deleteTitle'),
    message: t('employees.deleteConfirm', { name: row.nickName }),
    tone: 'danger',
  }))) return
  await deleteEmployee(row.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>

<style scoped>
.emp-name__main { display: block; font-weight: 600; }
.emp-name__sub { display: block; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.emp-freq { color: var(--nad-ink-soft, #64748b); font-size: 12px; }
</style>
