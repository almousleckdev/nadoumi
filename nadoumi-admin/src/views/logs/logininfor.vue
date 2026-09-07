<template>
  <div class="nad-page">
    <PageHeader
      :title="t('loginlog.title')"
      :subtitle="t('loginlog.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('monitor:logininfor:remove')"
          type="danger"
          plain
          :icon="Delete"
          @click="clean"
        >
          {{ t('loginlog.clean') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="Boolean(filters.userName || filters.status)"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.userName"
        :placeholder="t('loginlog.user')"
        clearable
        style="width: 200px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('loginlog.status')"
        clearable
        style="width: 130px"
        @change="reload"
      >
        <el-option
          :label="t('loginlog.ok')"
          value="0"
        />
        <el-option
          :label="t('loginlog.fail')"
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
      row-key="infoId"
      @retry="load"
    >
      <template #cell-status="{ row }">
        <StatusBadge
          :status="row.status === '0' ? 'ACTIVE' : 'FAILED'"
          :label="t(row.status === '0' ? 'loginlog.ok' : 'loginlog.fail')"
        />
      </template>
    </DataTable>

    <Pagination
      v-model:page="filters.pageNum"
      v-model:size="filters.pageSize"
      :total="total"
      @change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
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
import { listLoginLogs, cleanLoginLogs, type SysLogininfor } from '@/api/monitor'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const rows = ref<SysLogininfor[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const filters = reactive({ userName: '', status: '', pageNum: 1, pageSize: 10 })

const columns = computed(() => [
  { prop: 'userName', label: t('loginlog.user'), minWidth: 130 },
  { prop: 'ipaddr', label: 'IP', width: 140 },
  { prop: 'loginLocation', label: t('loginlog.location'), minWidth: 120 },
  { prop: 'browser', label: t('loginlog.browser'), width: 130 },
  { prop: 'os', label: t('loginlog.os'), width: 110 },
  { prop: 'status', label: t('loginlog.status'), width: 100, align: 'center' as const },
  { prop: 'msg', label: t('loginlog.message'), minWidth: 140 },
  { prop: 'loginTime', label: t('loginlog.time'), width: 170 },
])

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listLoginLogs({
      userName: filters.userName || undefined,
      status: filters.status || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      orderByColumn: 'infoId',
      isAsc: 'desc',
    })
    rows.value = res.rows
    total.value = res.total
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}
function reload() { filters.pageNum = 1; load() }
function clearFilters() { filters.userName = ''; filters.status = ''; reload() }

async function clean() {
  if (!(await confirm({ title: t('loginlog.clean'), message: t('loginlog.cleanConfirm'), tone: 'danger' }))) return
  await cleanLoginLogs()
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>
