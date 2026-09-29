<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { NotificationDetail } from '@/api/notification'
import type { Task } from '@/api/hr'
import { deliveryTone, parsePayload, payloadRows } from './notificationPayload'

const props = defineProps<{ detail: NotificationDetail | null, taskDetail: Task | null }>()
const emit = defineEmits<{ close: [] }>()

const { t } = useI18n()

const rows = computed(() => payloadRows(parsePayload(props.detail?.dataJson)))
const lastError = computed(() =>
  props.detail?.deliveries.map(d => d.lastError).filter(Boolean).join(' · ') || '')
</script>

<template>
  <el-drawer
    :model-value="Boolean(detail)"
    :title="t('notifications.detailTitle')"
    size="480"
    @update:model-value="emit('close')"
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
        v-else-if="rows.length"
        class="notif__meta"
      >
        <div
          v-for="r in rows"
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
</template>

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
