<template>
  <div class="nad-page">
    <PageHeader
      :title="t('scholarship.title')"
      :subtitle="t('scholarship.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:create')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('scholarship.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <SearchInput
        v-model="query.q"
        :placeholder="t('scholarship.searchPlaceholder')"
        @search="applyFilters"
      />
      <el-input
        v-model="query.country"
        :placeholder="t('scholarship.country')"
        maxlength="2"
        style="width: 110px"
        @keyup.enter="applyFilters"
      />
      <el-select
        v-model="query.funding"
        :placeholder="t('scholarship.fundingModel')"
        clearable
        style="width: 160px"
        @change="applyFilters"
      >
        <el-option
          v-for="f in FUNDING"
          :key="f"
          :value="f"
          :label="t(`scholarship.funding.${f}`)"
        />
      </el-select>
      <el-select
        v-model="query.publishStatus"
        :placeholder="t('scholarship.publishStatus')"
        clearable
        style="width: 150px"
        @change="applyFilters"
      >
        <el-option
          value="DRAFT"
          label="DRAFT"
        />
        <el-option
          value="PUBLISHED"
          label="PUBLISHED"
        />
      </el-select>
    </FilterBar>

    <DataTable
      storage-key="scholarships"
      :columns="columns"
      :rows="rows"
      :loading="loading"
      :error="error"
      :total="total"
      :page="query.page"
      :page-size="query.size"
      clickable-rows
      :empty-title="t('scholarship.emptyTitle')"
      :empty-description="t('scholarship.emptyDesc')"
      @update:page="(p: number) => { query.page = p; reload() }"
      @retry="reload"
      @row-click="(row) => router.push(`/scholarships/${row.view.id}`)"
    >
      <template #cell-referenceCode="{ row }">
        <span class="s-ref">{{ (row as Scholarship).view.referenceCode || '—' }}</span>
      </template>
      <template #cell-title="{ row }">
        <div class="s-title">
          <span>{{ (row as Scholarship).view.title }}</span>
          <el-tag
            v-if="(row as Scholarship).view.hot"
            size="small"
            type="warning"
            effect="plain"
            disable-transitions
          >
            {{ t('scholarship.hot') }}
          </el-tag>
          <el-tag
            v-else-if="(row as Scholarship).view.featured"
            size="small"
            type="primary"
            effect="plain"
            disable-transitions
          >
            {{ t('scholarship.featured') }}
          </el-tag>
        </div>
      </template>
      <template #cell-country="{ row }">
        <span class="mono">{{ (row as Scholarship).view.country }}</span>
      </template>
      <template #cell-funding="{ row }">
        {{ t(`scholarship.funding.${(row as Scholarship).view.fundingModel}`) }}
      </template>
      <template #cell-levels="{ row }">
        {{ (row as Scholarship).view.levels.map(l => t(`scholarship.level.${l}`)).join(', ') || '—' }}
      </template>
      <template #cell-categories="{ row }">
        {{ (row as Scholarship).view.categories.join(', ') || '—' }}
      </template>
      <template #cell-publishStatus="{ row }">
        <StatusBadge
          :status="(row as Scholarship).publishStatus"
          :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
        />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:edit')"
          link
          size="small"
          @click.stop="openEdit(row as Scholarship)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:remove')"
          link
          size="small"
          type="danger"
          @click.stop="onDelete(row as Scholarship)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <ScholarshipDrawer
      v-model="drawerOpen"
      :scholarship="editing"
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
import { listScholarships, deleteScholarship, type Scholarship } from '@/api/scholarship'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { DataTableColumn } from '@/components/ui/types'
import ScholarshipDrawer from './ScholarshipDrawer.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const FUNDING = ['FULLY', 'PARTIAL', 'SELF'] as const
const columns: DataTableColumn[] = [
  { prop: 'referenceCode', label: t('scholarship.referenceCode'), width: 130 },
  { prop: 'title', label: t('scholarship.title'), minWidth: 240 },
  { prop: 'country', label: t('scholarship.country'), width: 80, align: 'center' },
  { prop: 'funding', label: t('scholarship.fundingModel'), width: 130 },
  { prop: 'levels', label: t('scholarship.levels'), width: 180 },
  { prop: 'categories', label: t('scholarship.categories'), width: 150 },
  { prop: 'publishStatus', label: t('scholarship.publishStatus'), width: 120 },
  { prop: 'actions', label: '', width: 130, align: 'right' },
]

const loading = ref(false)
const error = ref<string | null>(null)
const rows = ref<Scholarship[]>([])
const total = ref(0)
const query = reactive({ q: '', country: '', funding: '', publishStatus: '', page: 0, size: 20 })
const dirty = computed(() => Boolean(query.q || query.country || query.funding || query.publishStatus))

async function reload() {
  loading.value = true
  error.value = null
  try {
    const res = await listScholarships({
      q: query.q || undefined,
      country: query.country || undefined,
      funding: query.funding || undefined,
      publishStatus: query.publishStatus || undefined,
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
function applyFilters() { query.page = 0; reload() }
function clearFilters() { query.q = ''; query.country = ''; query.funding = ''; query.publishStatus = ''; applyFilters() }

const drawerOpen = ref(false)
const editing = ref<Scholarship | null>(null)
function openCreate() { editing.value = null; drawerOpen.value = true }
function openEdit(s: Scholarship) { editing.value = s; drawerOpen.value = true }
function onSaved() { reload() }

async function onDelete(s: Scholarship) {
  const ok = await confirm({
    title: t('scholarship.deleteTitle'),
    message: t('scholarship.deleteConfirm', { name: s.view.title }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteScholarship(s.view.id)
  ElMessage.success(t('common.deleted'))
  reload()
}

onMounted(reload)
</script>

<style scoped>
.mono { font-variant-numeric: tabular-nums; letter-spacing: 0.03em; }
.s-title { display: flex; align-items: center; gap: 8px; }
.s-ref { font-variant-numeric: tabular-nums; font-size: 12px; color: var(--nad-ink-soft); }
</style>
