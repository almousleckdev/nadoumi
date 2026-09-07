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

    <el-drawer
      :model-value="Boolean(detail)"
      :title="t('notifications.detailTitle')"
      size="480"
      @update:model-value="detail = null"
    >
      <template v-if="detail">
        <h3 class="notif__h">
          {{ detail.title }}
        </h3>
        <p class="notif__body">
          {{ detail.body }}
        </p>

        <!-- rich task context for TASK_PROGRESS -->
        <section
          v-if="taskDetail"
          class="notif__task"
        >
          <div class="notif__task-head">
            <span class="notif__task-title">{{ taskDetail.title }}</span>
            <StatusBadge
              :status="taskDetail.priority === 'HIGH' ? 'ACTIVE' : taskDetail.priority === 'MEDIUM' ? 'SUBMITTED' : 'PENDING'"
              :label="t(`tasks.priorityMap.${taskDetail.priority}`)"
            />
            <StatusBadge
              :status="taskDetail.status === 'COMPLETED' || taskDetail.status === 'APPROVED' ? 'ACTIVE'
                : taskDetail.status === 'CANCELLED' ? 'FAILED' : 'PENDING'"
              :label="t(`tasks.statusMap.${taskDetail.status}`)"
            />
          </div>
          <dl class="notif__meta">
            <div>
              <dt>{{ t('tasks.assignee') }}</dt>
              <dd>{{ taskDetail.assigneeName || t('tasks.unassigned') }}</dd>
            </div>
            <div>
              <dt>{{ t('tasks.createdBy') }}</dt>
              <dd>{{ taskDetail.createdByName || `#${taskDetail.createdByUserId}` }}</dd>
            </div>
            <div>
              <dt>{{ t('tasks.dueDate') }}</dt>
              <dd>{{ taskDetail.dueDate || '' }}</dd>
            </div>
            <div>
              <dt>{{ t('notifications.startedAt') }}</dt>
              <dd>{{ taskDetail.startedAt || '' }}</dd>
            </div>
            <div>
              <dt>{{ t('notifications.completedAt') }}</dt>
              <dd>{{ taskDetail.completedAt || '' }}</dd>
            </div>
            <div v-if="taskDetail.approvedByName">
              <dt>{{ t('tasks.approvedBy') }}</dt>
              <dd>{{ taskDetail.approvedByName }}</dd>
            </div>
          </dl>
          <p
            v-if="taskDetail.description"
            class="notif__task-desc"
          >
            {{ taskDetail.description }}
          </p>
          <el-timeline v-if="taskDetail.events?.length">
            <el-timeline-item
              v-for="ev in taskDetail.events"
              :key="ev.id"
              :timestamp="ev.createdAt"
              placement="top"
            >
              <b>{{ t(`tasks.eventMap.${ev.eventType}`, ev.eventType) }}</b>
              <span v-if="ev.fromStatus || ev.toStatus">
                · {{ ev.fromStatus ? t(`tasks.statusMap.${ev.fromStatus}`, ev.fromStatus) : '' }}
                → {{ ev.toStatus ? t(`tasks.statusMap.${ev.toStatus}`, ev.toStatus) : '' }}
              </span>
              <span
                v-if="ev.actorName"
                class="notif__ev-actor"
              >{{ ev.actorName }}</span>
              <span
                v-if="ev.note"
                class="notif__ev-note"
              >{{ ev.note }}</span>
            </el-timeline-item>
          </el-timeline>
        </section>

        <!-- generic payload for the other event types -->
        <dl
          v-else-if="payloadRows.length"
          class="notif__meta"
        >
          <div
            v-for="r in payloadRows"
            :key="r.k"
          >
            <dt>{{ r.k }}</dt>
            <dd>{{ r.v }}</dd>
          </div>
        </dl>

        <dl class="notif__meta">
          <div>
            <dt>{{ t('notifications.type') }}</dt>
            <dd>{{ t(`notifications.typeMap.${detail.type}`, detail.type) }}</dd>
          </div>
          <div>
            <dt>{{ t('notifications.recipient') }}</dt>
            <dd>#{{ detail.recipientUserId }}</dd>
          </div>
          <div>
            <dt>{{ t('notifications.created') }}</dt>
            <dd>{{ detail.createdAt }}</dd>
          </div>
          <div>
            <dt>{{ t('notifications.readAt') }}</dt>
            <dd>{{ detail.readAt || '' }}</dd>
          </div>
        </dl>
        <h4
          v-if="detail.deliveries?.length"
          class="notif__h4"
        >
          {{ t('notifications.deliveries') }}
        </h4>
        <el-table
          v-if="detail.deliveries?.length"
          :data="detail.deliveries"
          size="small"
        >
          <el-table-column
            :label="t('notifications.channel')"
            prop="channel"
            width="90"
          />
          <el-table-column
            :label="t('notifications.status')"
            width="100"
          >
            <template #default="{ row }">
              <StatusBadge
                :status="deliveryTone(row.status)"
                :label="row.status"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="t('notifications.attempts')"
            prop="attempts"
            width="80"
            align="center"
          />
          <el-table-column
            :label="t('notifications.sentAt')"
            prop="sentAt"
            min-width="150"
          />
        </el-table>
        <p
          v-if="lastError"
          class="notif__err"
        >
          {{ lastError }}
        </p>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import {
  listStaffNotifications, getStaffNotification,
  listMyNotifications, markNotificationRead, markAllNotificationsRead,
  type NotificationView, type NotificationDetail,
} from '@/api/notification'
import { getTask, type Task } from '@/api/hr'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const { t } = useI18n()
const userStore = useUserStore()

const canOversee = computed(() => userStore.hasPerm('nad:notification:list'))
const mode = ref<'all' | 'mine'>(canOversee.value ? 'all' : 'mine')

const TYPES = ['CONTACT_INQUIRY_RECEIVED', 'SCHOLARSHIP_PUBLISHED', 'UNIVERSITY_PUBLISHED', 'PROGRAM_PUBLISHED', 'TASK_PROGRESS']

const rows = ref<NotificationView[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const loading = ref(false)
const error = ref<string | null>(null)
const detail = ref<NotificationDetail | null>(null)
const taskDetail = ref<Task | null>(null)
const filters = reactive({ type: '' })

const payloadObj = computed<Record<string, unknown>>(() => {
  try {
    return detail.value?.dataJson ? JSON.parse(detail.value.dataJson) : {}
  }
  catch { return {} }
})
// payload keys worth showing as plain rows (task rows get their own panel)
const payloadRows = computed(() => Object.entries(payloadObj.value)
  .filter(([k, v]) => k !== 'recipientUserIds' && v !== '' && v != null)
  .map(([k, v]) => ({ k, v: String(v) })))

const columns = computed(() => [
  { prop: 'type', label: t('notifications.type'), width: 200 },
  { prop: 'title', label: t('notifications.subject'), minWidth: 180 },
  { prop: 'body', label: t('notifications.preview'), minWidth: 240 },
  { prop: 'read', label: t('notifications.state'), width: 110, align: 'center' as const },
  { prop: 'createdAt', label: t('notifications.created'), width: 170 },
])

const lastError = computed(() =>
  detail.value?.deliveries.map(d => d.lastError).filter(Boolean).join(' · ') || '')

function deliveryTone(status: string): string {
  return status === 'SENT' || status === 'DELIVERED' ? 'ACTIVE' : status === 'FAILED' || status === 'BOUNCED' ? 'FAILED' : 'PENDING'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = mode.value === 'mine'
      ? await listMyNotifications({ page: page.value - 1, size: size.value })
      : await listStaffNotifications({
          type: filters.type || undefined,
          page: page.value - 1,
          size: size.value,
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
function reload() { page.value = 1; load() }
function clearFilters() { filters.type = ''; reload() }

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
    const id = Number(payloadObj.value.taskId)
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

<style scoped>
.notif__h { margin: 0 0 6px; font-size: 16px; }
.notif__body { margin: 0 0 16px; color: var(--nad-ink-soft, #64748b); white-space: pre-wrap; }
.notif__meta { display: grid; gap: 10px; margin: 0 0 18px; }
.notif__meta dt { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.notif__meta dd { margin: 2px 0 0; font-size: 14px; }
.notif__h4 { margin: 0 0 8px; font-size: 13px; }
.notif__err { margin-top: 10px; color: var(--nad-danger, #dc2626); font-size: 12px; white-space: pre-wrap; }
.notif__task {
  margin: 0 0 18px;
  padding: 12px;
  border: 1px solid var(--nad-line, #e5e7eb);
  border-radius: 10px;
  background: var(--nad-surface-2, #f8fafc);
}
.notif__task-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.notif__task-title { font-weight: 650; font-size: 14px; }
.notif__task-desc { margin: 4px 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); white-space: pre-wrap; }
.notif__ev-actor { margin-left: 6px; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.notif__ev-note { display: block; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
