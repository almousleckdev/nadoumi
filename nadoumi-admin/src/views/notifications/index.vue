<template>
  <div class="nad-page">
    <PageHeader
      :title="t('notifications.title')"
      :subtitle="canOversee ? t('notifications.subtitle') : t('notifications.mineSubtitle')"
    >
      <template #actions>
        <el-radio-group
          v-if="canOversee"
          v-model="mode"
          @change="reload"
        >
          <el-radio-button value="all">
            {{ t('notifications.viewAll') }}
          </el-radio-button>
          <el-radio-button value="mine">
            {{ t('notifications.mine') }}
          </el-radio-button>
        </el-radio-group>
        <el-button
          v-if="mode === 'mine'"
          @click="markAllRead"
        >
          {{ t('notifications.markAllRead') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      v-if="mode === 'all'"
      :dirty="Boolean(filters.type)"
      @clear="clearFilters"
    >
      <el-select
        v-model="filters.type"
        :placeholder="t('notifications.type')"
        clearable
        style="width: 240px"
        @change="reload"
      >
        <el-option
          v-for="ty in TYPES"
          :key="ty"
          :label="t(`notifications.typeMap.${ty}`, ty)"
          :value="ty"
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
      :empty-title="t('notifications.emptyTitle')"
      clickable-rows
      @retry="load"
      @row-click="(r) => openDetail(r as NotificationView)"
    >
      <template #cell-type="{ row }">
        {{ t(`notifications.typeMap.${row.type}`, row.type) }}
      </template>
      <template #cell-read="{ row }">
        <StatusBadge
          :status="row.read ? 'CLOSED' : 'PENDING'"
          :label="t(row.read ? 'notifications.read' : 'notifications.unread')"
        />
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <NotificationDetailDrawer
      :detail="detail"
      :task-detail="taskDetail"
      @close="detail = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import NotificationDetailDrawer from './NotificationDetailDrawer.vue'
import { parsePayload, taskIdOf } from './notificationPayload'
import {
  listStaffNotifications, getStaffNotification,
  listMyNotifications, markNotificationRead, markAllNotificationsRead,
  type NotificationView, type NotificationDetail,
} from '@/api/notification'
import { getTask, type Task } from '@/api/hr'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()
const userStore = useUserStore()

const canOversee = computed(() => userStore.hasPerm('nad:notification:list'))
const mode = ref<'all' | 'mine'>(canOversee.value ? 'all' : 'mine')

const TYPES = ['CONTACT_INQUIRY_RECEIVED', 'SCHOLARSHIP_PUBLISHED', 'UNIVERSITY_PUBLISHED', 'PROGRAM_PUBLISHED', 'TASK_PROGRESS']

const detail = ref<NotificationDetail | null>(null)
const taskDetail = ref<Task | null>(null)

const emptyFilters = () => ({ type: '' })
const { rows, total, loading, error, filters, page, size, load, reload, clearFilters } =
  usePagedList<NotificationView, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 1,
    size: 15,
    fetch: (f, { index, size }) => mode.value === 'mine'
      ? listMyNotifications({ page: index, size })
      : listStaffNotifications({ type: f.type || undefined, page: index, size }),
  })

const columns = computed(() => [
  { prop: 'type', label: t('notifications.type'), width: 200 },
  { prop: 'title', label: t('notifications.subject'), minWidth: 180 },
  { prop: 'body', label: t('notifications.preview'), minWidth: 240 },
  { prop: 'read', label: t('notifications.state'), width: 110, align: 'center' as const },
  { prop: 'createdAt', label: t('notifications.created'), width: 170 },
])

async function openDetail(row: NotificationView) {
  taskDetail.value = null
  if (mode.value === 'mine') {
    detail.value = { ...row, deliveries: [] } as unknown as NotificationDetail
    if (!row.read) {
      markNotificationRead(row.id).then(() => { row.read = true }).catch(() => {})
    }
  }
  else {
    detail.value = await getStaffNotification(row.id)
  }
  // TASK_PROGRESS carries only the id + status in its payload — pull the full task
  if (detail.value?.type === 'TASK_PROGRESS' && userStore.hasPerm('nad:task:query')) {
    const id = taskIdOf(parsePayload(detail.value.dataJson))
    if (id) {
      getTask(id).then(t => { taskDetail.value = t }).catch(() => {})
    }
  }
}

async function markAllRead() {
  await markAllNotificationsRead()
  ElMessage.success(t('common.saved'))
  load()
}

onMounted(load)
</script>
