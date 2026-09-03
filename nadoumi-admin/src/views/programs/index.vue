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
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="query.page"
      :page-size="query.size"
      :empty-title="t('program.emptyTitle')"
      :empty-description="t('program.emptyDesc')"
      @update:page="(p: number) => { query.page = p; reload() }"
      @retry="reload"
    >
      <template #cell-name="{ row }">
        <div class="p-name">
          <span>{{ row.name }}</span>
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
      </template>
      <template #cell-programType="{ value }">
        {{ t(`program.typeMap.${value}`) }}
      </template>
      <template #cell-teachingLanguage="{ value }">
        {{ value ? t(`program.langMap.${value}`) : '—' }}
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
  { prop: 'name', label: t('program.name'), minWidth: 240 },
  { prop: 'universityName', label: t('program.university'), minWidth: 180 },
  { prop: 'programType', label: t('program.type'), width: 130 },
  { prop: 'teachingLanguage', label: t('program.language'), width: 120 },
  { prop: 'publishStatus', label: t('program.publishStatus'), width: 120 },
  { prop: 'status', label: t('program.status'), width: 110 },
  { prop: 'actions', label: '', width: 130, align: 'right' },
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
  align-items: center;
  gap: 8px;
}
</style>
