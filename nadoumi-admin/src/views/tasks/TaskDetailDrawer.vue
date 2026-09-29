<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useUserStore } from '@/stores/user'
import type { Task } from '@/api/hr'
import { eventNodeType, isTerminal, priorityTone, statusTone, transitionsFor } from './taskWorkflow'

const props = defineProps<{ task: Task | null }>()
const emit = defineEmits<{ close: [], move: [to: string], edit: [], remove: [] }>()

const { t } = useI18n()
const userStore = useUserStore()

const canEdit = computed(() => userStore.hasPerm('nad:task:edit'))
const canRemove = computed(() => userStore.hasPerm('nad:task:remove'))
const moves = computed(() => props.task
  ? transitionsFor(props.task.status, {
    progress: userStore.hasPerm('nad:task:progress'),
    edit: canEdit.value,
    approve: userStore.hasPerm('nad:task:approve'),
  })
  : [])
</script>

<template>
  <el-drawer
    :model-value="Boolean(task)"
    :title="task ? `#${task.id} · ${task.title}` : ''"
    size="520"
    @update:model-value="emit('close')"
  >
    <template v-if="task">
      <div class="td-badges">
        <StatusBadge
          :status="priorityTone(task.priority)"
          :label="t(`tasks.priorityMap.${task.priority}`)"
        />
        <StatusBadge
          :status="statusTone(task.status)"
          :label="t(`tasks.statusMap.${task.status}`)"
        />
      </div>
      <p
        v-if="task.description"
        class="td-desc"
      >
        {{ task.description }}
      </p>
      <dl class="td-meta">
        <div>
          <dt>{{ t('tasks.assignee') }}</dt>
          <dd>{{ task.assigneeName || t('tasks.unassigned') }}</dd>
        </div>
        <div>
          <dt>{{ t('tasks.createdBy') }}</dt>
          <dd>{{ task.createdByName }}</dd>
        </div>
        <div>
          <dt>{{ t('tasks.dueDate') }}</dt>
          <dd>{{ task.dueDate || '' }}</dd>
        </div>
        <div v-if="task.approvedByName">
          <dt>{{ t('tasks.approvedBy') }}</dt>
          <dd>{{ task.approvedByName }} · {{ task.approvedAt }}</dd>
        </div>
      </dl>

      <div class="td-actions">
        <el-button
          v-for="tr in moves"
          :key="tr.to"
          :type="tr.type"
          size="small"
          @click="emit('move', tr.to)"
        >
          {{ t(`tasks.action.${tr.to}`) }}
        </el-button>
        <el-button
          v-if="canEdit && !isTerminal(task.status)"
          size="small"
          @click="emit('edit')"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="canRemove"
          size="small"
          type="danger"
          plain
          @click="emit('remove')"
        >
          {{ t('common.delete') }}
        </el-button>
      </div>

      <h4 class="td-h4">
        {{ t('tasks.timeline') }}
      </h4>
      <el-timeline class="td-timeline">
        <el-timeline-item
          v-for="ev in task.events"
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
</template>

<style scoped>
.td-badges { display: flex; gap: 8px; margin-bottom: 12px; }
.td-desc { white-space: pre-wrap; color: var(--nad-ink-soft, #64748b); margin: 0 0 16px; }
.td-meta { display: grid; gap: 10px; margin: 0 0 16px; }
.td-meta dt { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.td-meta dd { margin: 2px 0 0; font-size: 14px; }
.td-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 20px; }
.td-h4 { margin: 0 0 12px; font-size: 13px; }
.td-ev-meta { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
/* progress line: the connector between activity nodes is green so a run of
   status changes reads as forward movement. */
.td-timeline :deep(.el-timeline-item__tail) { border-left-color: var(--el-color-success); }
.td-timeline :deep(.el-timeline-item__node--success) { background-color: var(--el-color-success); }
</style>
