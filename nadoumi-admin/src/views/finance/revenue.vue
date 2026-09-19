<template>
  <div class="nad-page">
    <PageHeader
      :title="t('revenue.title')"
      :subtitle="t('revenue.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:revenue:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('revenue.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('revenue.searchPlaceholder')"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.source"
        :placeholder="t('revenue.source')"
        clearable
        style="width: 170px"
        @change="reload"
      >
        <el-option
          v-for="s in REVENUE_SOURCES"
          :key="s"
          :label="t(`revenue.sourceMap.${s}`)"
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
      :empty-title="t('revenue.emptyTitle')"
      @retry="load"
    >
      <template #cell-title="{ row }">
        <div class="rv-title">
          <span class="rv-title__main">{{ (row as Revenue).title }}</span>
          <span class="rv-title__sub">
            {{ t(`revenue.sourceMap.${(row as Revenue).source}`, (row as Revenue).source) }}
            <template v-if="(row as Revenue).reference"> · {{ (row as Revenue).reference }}</template>
          </span>
        </div>
      </template>
      <template #cell-amount="{ row }">
        {{ dualMoney((row as Revenue).amount, null) }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="openEdit(row as Revenue)"
        >
          {{ userStore.hasPerm('nad:revenue:edit') ? t('common.edit') : t('common.view') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('nad:revenue:remove')"
          link
          type="danger"
          @click="doDelete(row as Revenue)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <RevenueDrawer
      v-model="drawerOpen"
      :revenue-id="editingId"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import RevenueDrawer from './RevenueDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { dualMoney } from '@/utils/money'
import { listRevenue, deleteRevenue, REVENUE_SOURCES, type Revenue } from '@/api/finance'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const emptyFilters = () => ({ q: '', source: '' })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Revenue, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 1,
    size: 20,
    fetch: (f, { index, size }) => listRevenue({
      q: f.q || undefined,
      source: f.source || undefined,
      page: index,
      size,
    }),
  })

const drawerOpen = ref(false)
const editingId = ref<number | undefined>()

const columns = computed(() => [
  { prop: 'title', label: t('revenue.revenueTitle'), minWidth: 260 },
  { prop: 'amount', label: t('revenue.amount'), width: 130, align: 'right' as const },
  { prop: 'receivedOn', label: t('revenue.receivedOn'), width: 130 },
  { prop: 'recordedByName', label: t('revenue.recordedBy'), minWidth: 140 },
  { prop: 'actions', label: t('common.actions'), width: 150, align: 'right' as const },
])

function openCreate() { editingId.value = undefined; drawerOpen.value = true }
function openEdit(r: Revenue) { editingId.value = r.id; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load() }

async function doDelete(r: Revenue) {
  if (!(await confirm({
    title: t('revenue.deleteTitle'),
    message: t('revenue.deleteConfirm', { title: r.title }),
    tone: 'danger',
  }))) return
  await deleteRevenue(r.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>

<style scoped>
.rv-title__main { display: block; font-weight: 600; }
.rv-title__sub { display: block; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
</style>
