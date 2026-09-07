<template>
  <div class="nad-page">
    <PageHeader
      :title="t('university.title')"
      :subtitle="t('university.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:university:create')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('university.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <SearchInput
        v-model="query.q"
        :placeholder="t('university.searchPlaceholder')"
        @search="applyFilters"
      />
      <el-input
        v-model="query.country"
        :placeholder="t('university.country')"
        maxlength="2"
        style="width: 120px"
        @keyup.enter="applyFilters"
      />
      <el-select
        v-model="query.status"
        :placeholder="t('university.status')"
        clearable
        style="width: 150px"
        @change="applyFilters"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="titleCase(s)"
          :value="s"
        />
      </el-select>
    </FilterBar>

    <DataTable
      storage-key="universities"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="query.page"
      :page-size="query.size"
      clickable-rows
      :empty-title="t('university.emptyTitle')"
      :empty-description="t('university.emptyDesc')"
      @update:page="(p: number) => { query.page = p; reload() }"
      @update:page-size="(s: number) => { query.size = s; query.page = 0; reload() }"
      @retry="reload"
      @row-click="(row) => router.push(`/universities/${row.id}`)"
    >
      <template #cell-name="{ row }">
        <div class="uni-row">
          <div
            class="uni-logo"
            :class="{ 'uni-logo--empty': !row.logoUrl && !row.logoImageUrl }"
          >
            <img
              v-if="row.logoUrl || row.logoImageUrl"
              :src="assetUrl(row.logoUrl ?? row.logoImageUrl)"
              alt=""
            >
            <span v-else>{{ (row.name || '?').charAt(0) }}</span>
          </div>
          <div class="uni-name">
            <div class="uni-name__top">
              <span>{{ row.name }}</span>
              <el-tag
                v-if="row.featured"
                size="small"
                type="warning"
                effect="plain"
                disable-transitions
              >
                {{ t('university.featured') }}
              </el-tag>
              <el-tag
                v-if="row.partnerStatus === 'PARTNER'"
                size="small"
                type="success"
                effect="plain"
                disable-transitions
              >
                {{ t('university.partnerTag') }}
              </el-tag>
            </div>
            <span class="uni-name__ref">{{ row.referenceCode || '' }}</span>
          </div>
        </div>
      </template>
      <template #cell-country="{ value }">
        <span class="mono">{{ value }}</span>
      </template>
      <template #cell-type="{ value }">
        {{ value ? (value[0] + value.slice(1).toLowerCase()) : '' }}
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
          v-if="userStore.hasPerm('nad:university:edit')"
          link
          size="small"
          @click.stop="openEdit(row as University)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:university:remove')"
          link
          size="small"
          type="danger"
          @click.stop="onDelete(row as University)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <UniversityDrawer
      v-model="drawerOpen"
      :university="editing"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listUniversities, deleteUniversity,
  type University, type UniversityStatus,
} from '@/api/university'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import { assetUrl } from '@/utils/asset'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { DataTableColumn } from '@/components/ui/types'
import UniversityDrawer from './UniversityDrawer.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const STATUSES: UniversityStatus[] = ['ACTIVE', 'INACTIVE']
const columns: DataTableColumn[] = [
  { prop: 'name', label: t('university.name'), minWidth: 300 },
  { prop: 'country', label: t('university.country'), width: 90, align: 'center' },
  { prop: 'city', label: t('university.city'), width: 140 },
  { prop: 'type', label: t('university.type'), width: 100 },
  { prop: 'publishStatus', label: t('university.publishStatus'), width: 120 },
  { prop: 'status', label: t('university.status'), width: 120 },
  { prop: 'actions', label: '', width: 130, align: 'right' },
]

const loading = ref(false)
const error = ref<string | null>(null)
const rows = ref<University[]>([])
const total = ref(0)
const query = reactive({ q: '', country: '', status: '', page: 0, size: 20 })
const dirty = computed(() => Boolean(query.q || query.country || query.status))

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

async function reload() {
  loading.value = true
  error.value = null
  try {
    const res = await listUniversities({
      q: query.q || undefined,
      country: query.country || undefined,
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
  query.country = ''
  query.status = ''
  applyFilters()
}

const drawerOpen = ref(false)
const editing = ref<University | null>(null)
function openCreate() {
  editing.value = null
  drawerOpen.value = true
}
function openEdit(u: University) {
  editing.value = u
  drawerOpen.value = true
}
function onSaved() {
  reload()
}

async function onDelete(u: University) {
  const ok = await confirm({
    title: t('university.deleteTitle'),
    message: t('university.deleteConfirm', { name: u.name }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteUniversity(u.id)
  ElMessage.success(t('common.deleted'))
  reload()
}

onMounted(reload)
</script>

<style scoped>
.mono {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.03em;
}
.uni-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.uni-logo {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 7px;
  overflow: hidden;
  border: 1px solid var(--nad-line, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.uni-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.uni-logo--empty {
  background: var(--nad-surface-2, #f1f5f9);
  color: var(--nad-ink-soft, #64748b);
  font-weight: 700;
  font-size: 14px;
}
.uni-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.uni-name__top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.uni-name__ref {
  font-size: 12px;
  color: var(--nad-ink-soft, #64748b);
  font-variant-numeric: tabular-nums;
}
</style>

