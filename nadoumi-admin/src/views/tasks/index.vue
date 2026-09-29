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
      :dirty="dirty"
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
          v-for="s in TASK_STATUSES"
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
          v-for="p in TASK_PRIORITIES"
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
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <TaskDrawer
      v-model="drawerOpen"
      :task-id="editingId"
      @saved="onSaved"
    />

    <TaskDetailDrawer
      :task="detail"
      @close="detail = null"
      @move="to => detail && move(detail, to)"
      @edit="detail && editFromDetail(detail)"
      @remove="detail && removeTask(detail)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import TaskDrawer from './TaskDrawer.vue'
import TaskDetailDrawer from './TaskDetailDrawer.vue'
import { isOverdue, priorityTone, statusTone, TASK_PRIORITIES, TASK_STATUSES } from './taskWorkflow'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listTasks, getTask, changeTaskStatus, deleteTask, type Task } from '@/api/hr'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const emptyFilters = () => ({ q: '', status: '', priority: '', mine: false })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Task, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 1,
    size: 20,
    fetch: (f, { index, size }) => listTasks({
      q: f.q || undefined,
      status: f.status || undefined,
      priority: f.priority || undefined,
      mine: f.mine || undefined,
      page: index,
      size,
    }),
  })

const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const detail = ref<Task | null>(null)

const columns = computed(() => [
  { prop: 'title', label: t('tasks.taskTitle'), minWidth: 220 },
  { prop: 'priority', label: t('tasks.priority'), width: 120, align: 'center' as const },
  { prop: 'status', label: t('tasks.status'), width: 130, align: 'center' as const },
  { prop: 'assigneeName', label: t('tasks.assignee'), minWidth: 140 },
  { prop: 'dueDate', label: t('tasks.dueDate'), width: 120 },
  { prop: 'createdByName', label: t('tasks.createdBy'), minWidth: 130 },
])

function openCreate() { editingId.value = undefined; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load(); if (detail.value) openDetail(detail.value.id) }

async function openDetail(id: number) { detail.value = await getTask(id) }
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
  await openDetail(task.id)
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
.task-overdue { color: var(--nad-danger, #dc2626); font-weight: 600; }
</style>
