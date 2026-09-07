<template>
  <div class="nad-page">
    <PageHeader
      :title="t('tasks.title')"
      :subtitle="t('tasks.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('nad:task:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('tasks.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="Boolean(filters.q || filters.status || filters.priority || filters.mine)"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('tasks.searchPlaceholder')"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('tasks.status')"
        clearable
        style="width: 150px"
        @change="reload"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="t(`tasks.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="filters.priority"
        :placeholder="t('tasks.priority')"
        clearable
        style="width: 140px"
        @change="reload"
      >
        <el-option
          v-for="p in PRIORITIES"
          :key="p"
          :label="t(`tasks.priorityMap.${p}`)"
          :value="p"
        />
      </el-select>
      <el-checkbox
        v-model="filters.mine"
        @change="reload"
      >
        {{ t('tasks.createdByMe') }}
      </el-checkbox>
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
      :empty-title="t('tasks.emptyTitle')"
      clickable-rows
      @retry="load"
      @row-click="(r) => openDetail((r as Task).id)"
    >
      <template #cell-priority="{ row }">
        <StatusBadge
          :status="priorityTone((row as Task).priority)"
          :label="t(`tasks.priorityMap.${(row as Task).priority}`)"
        />
      </template>
      <template #cell-status="{ row }">
        <StatusBadge
          :status="statusTone((row as Task).status)"
          :label="t(`tasks.statusMap.${(row as Task).status}`)"
        />
      </template>
      <template #cell-dueDate="{ row }">
        <span :class="{ 'task-overdue': isOverdue(row as Task) }">{{ (row as Task).dueDate || '' }}</span>
      </template>
    </DataTable>

    <Pagination
      v-model:page="filters.page"
      v-model:size="filters.size"
      :total="total"
      @change="load"
    />

    <TaskDrawer
      v-model="drawerOpen"
      :task-id="editingId"
      @saved="onSaved"
    />

    <el-drawer
      :model-value="Boolean(detail)"
      :title="detail ? `#${detail.id} · ${detail.title}` : ''"
      size="520"
      @update:model-value="detail = null"
    >
      <template v-if="detail">
        <div class="td-badges">
          <StatusBadge
            :status="priorityTone(detail.priority)"
            :label="t(`tasks.priorityMap.${detail.priority}`)"
          />
          <StatusBadge
            :status="statusTone(detail.status)"
            :label="t(`tasks.statusMap.${detail.status}`)"
          />
        </div>
        <p
          v-if="detail.description"
          class="td-desc"
        >
          {{ detail.description }}
        </p>
        <dl class="td-meta">
          <div>
            <dt>{{ t('tasks.assignee') }}</dt>
            <dd>{{ detail.assigneeName || t('tasks.unassigned') }}</dd>
          </div>
          <div>
            <dt>{{ t('tasks.createdBy') }}</dt>
            <dd>{{ detail.createdByName }}</dd>
          </div>
          <div>
            <dt>{{ t('tasks.dueDate') }}</dt>
            <dd>{{ detail.dueDate || '' }}</dd>
          </div>
          <div v-if="detail.approvedByName">
            <dt>{{ t('tasks.approvedBy') }}</dt>
            <dd>{{ detail.approvedByName }} · {{ detail.approvedAt }}</dd>
          </div>
        </dl>

        <div class="td-actions">
          <el-button
            v-for="tr in transitions(detail)"
            :key="tr.to"
            :type="tr.type"
            size="small"
            @click="move(detail, tr.to)"
          >
            {{ t(`tasks.action.${tr.to}`) }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('nad:task:edit') && !isTerminal(detail.status)"
            size="small"
            @click="editFromDetail(detail)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('nad:task:remove')"
            size="small"
            type="danger"
            plain
            @click="removeTask(detail)"
          >
            {{ t('common.delete') }}
          </el-button>
        </div>

        <h4 class="td-h4">
          {{ t('tasks.timeline') }}
        </h4>
        <el-timeline class="td-timeline">
          <el-timeline-item
            v-for="ev in detail.events"
            :key="ev.id"
            :timestamp="ev.createdAt"
            :type="eventNodeType(ev.eventType)"
            :hollow="ev.eventType !== 'STATUS_CHANGED' && ev.eventType !== 'CREATED'"
            placement="top"
          >
            <b>{{ t(`tasks.eventMap.${ev.eventType}`, ev.eventType) }}</b>
            <span v-if="ev.fromStatus || ev.toStatus">
              · {{ ev.fromStatus ? t(`tasks.statusMap.${ev.fromStatus}`) : '' }}
              → {{ ev.toStatus ? t(`tasks.statusMap.${ev.toStatus}`) : '' }}
            </span>
            <div class="td-ev-meta">
              {{ ev.actorName }}<span v-if="ev.note"> · {{ ev.note }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import TaskDrawer from './TaskDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listTasks, getTask, changeTaskStatus, deleteTask, type Task } from '@/api/hr'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const STATUSES = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'APPROVED', 'CANCELLED']
const PRIORITIES = ['HIGH', 'MEDIUM', 'LOW']

const rows = ref<Task[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const detail = ref<Task | null>(null)
const filters = reactive({ q: '', status: '', priority: '', mine: false, page: 1, size: 20 })

const columns = computed(() => [
  { prop: 'title', label: t('tasks.taskTitle'), minWidth: 220 },
  { prop: 'priority', label: t('tasks.priority'), width: 120, align: 'center' as const },
  { prop: 'status', label: t('tasks.status'), width: 130, align: 'center' as const },
  { prop: 'assigneeName', label: t('tasks.assignee'), minWidth: 140 },
  { prop: 'dueDate', label: t('tasks.dueDate'), width: 120 },
  { prop: 'createdByName', label: t('tasks.createdBy'), minWidth: 130 },
])

// product spec: LOW = orange, MEDIUM = blue, HIGH = green
function priorityTone(p: string): string {
  return p === 'LOW' ? 'PENDING' : p === 'MEDIUM' ? 'SUBMITTED' : 'ACTIVE'
}
function statusTone(s: string): string {
  return s === 'APPROVED' ? 'ACTIVE' : s === 'IN_PROGRESS' ? 'IN_REVIEW'
    : s === 'COMPLETED' ? 'PENDING' : s === 'CANCELLED' ? 'FAILED' : 'DRAFT'
}
function isTerminal(s: string) { return s === 'APPROVED' || s === 'CANCELLED' }
// a status-change (or the initial CREATED) is a real progress step — solid green node;
// everything else (assignment, edits, priority) is a hollow secondary marker.
function eventNodeType(type: string): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'STATUS_CHANGED' || type === 'CREATED') return 'success'
  if (type === 'ASSIGNED') return 'primary'
  if (type === 'PRIORITY_CHANGED') return 'warning'
  return 'info'
}
function isOverdue(task: Task) {
  return task.dueDate != null && !isTerminal(task.status) && task.dueDate < new Date().toISOString().slice(0, 10)
}

interface Transition { to: string, type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }
function transitions(task: Task): Transition[] {
  // a rank-and-file employee gets nad:task:progress (status only); editors get nad:task:edit
  if (!userStore.hasPerm('nad:task:progress') && !userStore.hasPerm('nad:task:edit')) return []
  switch (task.status) {
    case 'PENDING': return [{ to: 'IN_PROGRESS', type: 'primary' }, { to: 'CANCELLED', type: 'danger' }]
    case 'IN_PROGRESS': return [{ to: 'COMPLETED', type: 'success' }, { to: 'PENDING', type: 'info' }, { to: 'CANCELLED', type: 'danger' }]
    case 'COMPLETED': {
      const rows_: Transition[] = [{ to: 'IN_PROGRESS', type: 'warning' }, { to: 'CANCELLED', type: 'danger' }]
      if (userStore.hasPerm('nad:task:approve')) rows_.unshift({ to: 'APPROVED', type: 'success' })
      return rows_
    }
    default: return []
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listTasks({
      q: filters.q || undefined,
      status: filters.status || undefined,
      priority: filters.priority || undefined,
      mine: filters.mine || undefined,
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
function clearFilters() {
  filters.q = ''; filters.status = ''; filters.priority = ''; filters.mine = false
  reload()
}
function openCreate() { editingId.value = undefined; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load(); if (detail.value) refreshDetail(detail.value.id) }

async function openDetail(id: number) { detail.value = await getTask(id) }
async function refreshDetail(id: number) { detail.value = await getTask(id) }
function editFromDetail(task: Task) { editingId.value = task.id; drawerOpen.value = true }

async function move(task: Task, to: string) {
  let note: string | undefined
  if (to === 'CANCELLED' || to === 'PENDING' || to === 'IN_PROGRESS') {
    try {
      const r = await ElMessageBox.prompt(t('tasks.notePrompt'), t(`tasks.action.${to}`), {
        inputType: 'textarea', inputValue: '',
      })
      note = r.value || undefined
    }
    catch { return }
  }
  await changeTaskStatus(task.id, to, note)
  ElMessage.success(t('common.saved'))
  await refreshDetail(task.id)
  load()
}

async function removeTask(task: Task) {
  if (!(await confirm({
    title: t('tasks.deleteTitle'),
    message: t('tasks.deleteConfirm', { title: task.title }),
    tone: 'danger',
  }))) return
  await deleteTask(task.id)
  ElMessage.success(t('common.deleted'))
  detail.value = null
  load()
}

onMounted(load)
</script>

<style scoped>
.td-badges { display: flex; gap: 8px; margin-bottom: 12px; }
.td-desc { white-space: pre-wrap; color: var(--nad-ink-soft, #64748b); margin: 0 0 16px; }
.td-meta { display: grid; gap: 10px; margin: 0 0 16px; }
.td-meta dt { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.td-meta dd { margin: 2px 0 0; font-size: 14px; }
.td-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 20px; }
.td-h4 { margin: 0 0 12px; font-size: 13px; }
.td-ev-meta { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
.task-overdue { color: var(--nad-danger, #dc2626); font-weight: 600; }
/* progress line: the connector between activity nodes is green so a run of
   status changes reads as forward movement. */
.td-timeline :deep(.el-timeline-item__tail) { border-left-color: var(--el-color-success); }
.td-timeline :deep(.el-timeline-item__node--success) { background-color: var(--el-color-success); }
</style>
