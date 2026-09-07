<template>
  <div class="nad-page">
    <PageHeader
      :title="t('program.title')"
      :subtitle="t('program.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:program:create')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('program.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <SearchInput
        v-model="query.q"
        :placeholder="t('program.searchPlaceholder')"
        @search="applyFilters"
      />
      <el-select
        v-model="query.universityId"
        :placeholder="t('program.university')"
        filterable
        clearable
        style="width: 220px"
        @change="applyFilters"
      >
        <el-option
          v-for="u in universities"
          :key="u.id"
          :label="u.name"
          :value="u.id"
        />
      </el-select>
      <el-select
        v-model="query.type"
        :placeholder="t('program.type')"
        clearable
        style="width: 160px"
        @change="applyFilters"
      >
        <el-option
          v-for="pt in PROGRAM_TYPES"
          :key="pt"
          :label="t(`program.typeMap.${pt}`)"
          :value="pt"
        />
      </el-select>
      <el-select
        v-model="query.status"
        :placeholder="t('program.status')"
        clearable
        style="width: 140px"
        @change="applyFilters"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="t(`program.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
    </FilterBar>

    <DataTable
      storage-key="programs"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="query.page"
      :page-size="query.size"
      :clickable-rows="userStore.hasPerm('nad:program:edit')"
      :empty-title="t('program.emptyTitle')"
      :empty-description="t('program.emptyDesc')"
      @update:page="(p: number) => { query.page = p; reload() }"
      @update:page-size="(s: number) => { query.size = s; query.page = 0; reload() }"
      @row-click="(row) => openEdit(row as Program)"
      @retry="reload"
    >
      <template #cell-name="{ row }">
        <div class="p-name">
          <div class="p-name__top">
            <span class="p-name__title">{{ row.name }}</span>
            <el-tag
              v-if="row.hot"
              size="small"
              type="danger"
              effect="plain"
              disable-transitions
            >
              {{ t('program.hot') }}
            </el-tag>
            <el-tag
              v-if="row.featured"
              size="small"
              type="warning"
              effect="plain"
              disable-transitions
            >
              {{ t('program.featured') }}
            </el-tag>
          </div>
          <span class="p-name__ref">#{{ row.id }} · {{ row.slug }}</span>
        </div>
      </template>
      <template #cell-programType="{ row }">
        {{ (row as Program).programType === 'DEGREE' && (row as Program).levels?.length
          ? (row as Program).levels.map(l => t(`program.levelMap.${l}`)).join(', ')
          : t(`program.typeMap.${(row as Program).programType}`) }}
      </template>
      <template #cell-teachingLanguage="{ value }">
        {{ value ? t(`program.langMap.${value}`) : '' }}
      </template>
      <template #cell-tuition="{ row }">
        {{ dualMoney(row.tuitionAmount, row.tuitionAmountUsd) }}
      </template>
      <template #cell-publishStatus="{ value }">
        <StatusBadge
          :status="value"
          :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
        />
      </template>
      <template #cell-status="{ value }">
        <StatusBadge :status="value" />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="userStore.hasPerm('nad:program:edit')"
          link
          size="small"
          @click.stop="openEdit(row as Program)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:program:remove')"
          link
          size="small"
          type="danger"
          @click.stop="onDelete(row as Program)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <ProgramDrawer
      v-model="drawerOpen"
      :program="editing"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listPrograms, deleteProgram, PROGRAM_TYPES,
  type Program, type ProgramStatus,
} from '@/api/program'
import { listUniversities, type University } from '@/api/university'
import { dualMoney } from '@/utils/money'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { DataTableColumn } from '@/components/ui/types'
import ProgramDrawer from './ProgramDrawer.vue'

const { t } = useI18n()
const userStore = useUserStore()
const { confirm } = useConfirm()

const STATUSES: ProgramStatus[] = ['ACTIVE', 'INACTIVE']
const columns: DataTableColumn[] = [
  { prop: 'name', label: t('program.name'), minWidth: 220 },
  { prop: 'universityName', label: t('program.university'), minWidth: 150 },
  { prop: 'programType', label: t('program.type'), minWidth: 130 },
  { prop: 'teachingLanguage', label: t('program.language'), width: 110 },
  { prop: 'tuition', label: t('program.tuitionAmount'), width: 140 },
  { prop: 'publishStatus', label: t('program.publishStatus'), width: 110 },
  { prop: 'status', label: t('program.status'), width: 100 },
  { prop: 'actions', label: t('common.actions'), width: 120, align: 'right' },
]

const loading = ref(false)
const error = ref<string | null>(null)
const rows = ref<Program[]>([])
const total = ref(0)
const universities = ref<{ id: number, name: string }[]>([])
const query = reactive({
  q: '', universityId: undefined as number | undefined, type: '', status: '', page: 0, size: 20,
})
const dirty = computed(() =>
  Boolean(query.q || query.universityId || query.type || query.status))

listUniversities({ page: 0, size: 200 })
  .then(res => universities.value = res.content.map((u: University) => ({ id: u.id, name: u.name })))
  .catch(() => {})

async function reload() {
  loading.value = true
  error.value = null
  try {
    const res = await listPrograms({
      q: query.q || undefined,
      universityId: query.universityId,
      type: query.type || undefined,
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.content
    total.value = res.totalElements
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}
function applyFilters() {
  query.page = 0
  reload()
}
function clearFilters() {
  query.q = ''
  query.universityId = undefined
  query.type = ''
  query.status = ''
  applyFilters()
}

const drawerOpen = ref(false)
const editing = ref<Program | null>(null)
function openCreate() {
  editing.value = null
  drawerOpen.value = true
}
function openEdit(p: Program) {
  editing.value = p
  drawerOpen.value = true
}
function onSaved() {
  reload()
}

async function onDelete(p: Program) {
  const ok = await confirm({
    title: t('program.deleteTitle'),
    message: t('program.deleteConfirm', { name: p.name }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteProgram(p.id)
  ElMessage.success(t('common.deleted'))
  reload()
}

onMounted(reload)
</script>

<style scoped>
.p-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.p-name__top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.p-name__title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.p-name__ref {
  font-size: 12px;
  color: var(--nad-ink-soft, #64748b);
  font-variant-numeric: tabular-nums;
}
</style>
