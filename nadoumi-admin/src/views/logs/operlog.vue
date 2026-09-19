<template>
  <div class="nad-page">
    <PageHeader
      :title="t('operlog.title')"
      :subtitle="t('operlog.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('monitor:operlog:remove')"
          type="danger"
          plain
          :icon="Delete"
          @click="clean"
        >
          {{ t('operlog.clean') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.title"
        :placeholder="t('operlog.module')"
        clearable
        style="width: 180px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-input
        v-model="filters.operName"
        :placeholder="t('operlog.operator')"
        clearable
        style="width: 180px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('operlog.status')"
        clearable
        style="width: 130px"
        @change="reload"
      >
        <el-option
          :label="t('operlog.ok')"
          value="0"
        />
        <el-option
          :label="t('operlog.fail')"
          value="1"
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
      row-key="operId"
      @retry="load"
    >
      <template #cell-businessType="{ row }">
        {{ t(`operlog.biz.${row.businessType}`, String(row.businessType)) }}
      </template>
      <template #cell-status="{ row }">
        <StatusBadge
          :status="row.status === 0 ? 'ACTIVE' : 'FAILED'"
          :label="t(row.status === 0 ? 'operlog.ok' : 'operlog.fail')"
        />
      </template>
      <template #cell-costTime="{ row }">
        {{ row.costTime }} ms
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="detail = (row as SysOperLog)"
        >
          {{ t('operlog.detail') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <el-dialog
      :model-value="Boolean(detail)"
      :title="t('operlog.detail')"
      width="620"
      @update:model-value="detail = null"
    >
      <template v-if="detail">
        <p><b>{{ t('operlog.module') }}:</b> {{ detail.title }}</p>
        <p><b>URL:</b> {{ detail.requestMethod }} {{ detail.operUrl }}</p>
        <p><b>{{ t('operlog.method') }}:</b> {{ detail.method }}</p>
        <p><b>{{ t('operlog.operator') }}:</b> {{ detail.operName }} · {{ detail.operIp }} · {{ detail.operLocation }}</p>
        <p><b>{{ t('operlog.time') }}:</b> {{ detail.operTime }} ({{ detail.costTime }} ms)</p>
        <p
          v-if="detail.errorMsg"
          class="operlog__err"
        >
          {{ detail.errorMsg }}
        </p>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Delete, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listOperLogs, cleanOperLogs, type SysOperLog } from '@/api/monitor'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const detail = ref<SysOperLog | null>(null)

const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<SysOperLog, { title: string, operName: string, status: string }>({
    emptyFilters: () => ({ title: '', operName: '', status: '' }),
    firstPage: 1,
    size: 10,
    fetch: (f, { page, size }) => listOperLogs({
      title: f.title || undefined,
      operName: f.operName || undefined,
      status: f.status || undefined,
      pageNum: page,
      pageSize: size,
      orderByColumn: 'operId',
      isAsc: 'desc',
    }),
  })

const columns = computed(() => [
  { prop: 'title', label: t('operlog.module'), minWidth: 130 },
  { prop: 'businessType', label: t('operlog.action'), width: 110 },
  { prop: 'operName', label: t('operlog.operator'), width: 120 },
  { prop: 'operIp', label: 'IP', width: 130 },
  { prop: 'status', label: t('operlog.status'), width: 100, align: 'center' as const },
  { prop: 'costTime', label: t('operlog.cost'), width: 100, align: 'right' as const },
  { prop: 'operTime', label: t('operlog.time'), width: 170 },
  { prop: 'actions', label: t('common.actions'), width: 90, align: 'right' as const },
])

async function clean() {
  if (!(await confirm({ title: t('operlog.clean'), message: t('operlog.cleanConfirm'), tone: 'danger' }))) return
  await cleanOperLogs()
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>

<style scoped>
.operlog__err {
  white-space: pre-wrap;
  color: var(--nad-danger, #dc2626);
  font-size: 12px;
}
</style>
