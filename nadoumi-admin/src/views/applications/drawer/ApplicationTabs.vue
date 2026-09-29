<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { ApplicationDetail } from '@/api/application'
import { useApplicationLabels } from './useApplicationLabels'

const TASK_TONES = { OPEN: 'warning', DONE: 'success', SKIPPED: 'neutral', CANCELLED: 'neutral' } as const

defineProps<{ detail: ApplicationDetail, canTransition: boolean }>()
const emit = defineEmits<{ complete: [taskId: number], skip: [taskId: number] }>()
const tab = defineModel<string>('tab', { required: true })

const { t } = useI18n()
const { taskStatusLabel } = useApplicationLabels()
</script>

<template>
  <el-tabs v-model="tab">
    <el-tab-pane
      :label="t('applications.tasks')"
      name="tasks"
    >
      <p
        v-if="!detail.tasks.length"
        class="ad-empty"
      >
        {{ t('applications.noTasks') }}
      </p>
      <ul
        v-else
        class="ad-list"
      >
        <li
          v-for="task in detail.tasks"
          :key="task.id"
        >
          <div class="ad-row">
            <span>
              {{ task.title }}
              <el-tag
                v-if="task.mandatory"
                size="small"
                type="warning"
              >{{ t('applications.mandatory') }}</el-tag>
            </span>
            <StatusBadge
              :status="task.status"
              :label="taskStatusLabel(task.status)"
              :map="TASK_TONES"
            />
          </div>
          <div
            v-if="task.skipReason"
            class="ad-meta-line"
          >
            {{ t('applications.skipReason', { reason: task.skipReason }) }}
          </div>
          <div
            v-if="canTransition && task.status === 'OPEN'"
            class="ad-task-actions"
          >
            <el-button
              size="small"
              data-test="task-complete"
              @click="emit('complete', task.id)"
            >
              {{ t('applications.completeTask') }}
            </el-button>
            <el-button
              v-if="!task.mandatory"
              size="small"
              plain
              data-test="task-skip"
              @click="emit('skip', task.id)"
            >
              {{ t('applications.skipTask') }}
            </el-button>
          </div>
        </li>
      </ul>
    </el-tab-pane>

    <el-tab-pane
      :label="t('applications.history')"
      name="history"
    >
      <p
        v-if="!detail.history.length"
        class="ad-empty"
      >
        {{ t('applications.noHistory') }}
      </p>
      <el-timeline v-else>
        <el-timeline-item
          v-for="h in detail.history"
          :key="h.id"
          :timestamp="h.changedAt || ''"
          placement="top"
        >
          <b>{{ h.transitionCode }}</b>
          <div class="ad-meta-line">
            {{ t('applications.byUser', { id: h.changedBy ?? '-' }) }}<span v-if="h.reason"> · {{ h.reason }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-tab-pane>

    <el-tab-pane
      :label="t('applications.decisions')"
      name="decisions"
    >
      <p
        v-if="!detail.decisions.length"
        class="ad-empty"
      >
        {{ t('applications.noDecisions') }}
      </p>
      <ul
        v-else
        class="ad-list"
      >
        <li
          v-for="d in detail.decisions"
          :key="d.id"
        >
          <b>{{ d.decisionType }}: {{ d.outcome }}</b>
          <div class="ad-meta-line">
            {{ d.rationale }}
          </div>
          <div class="ad-meta-line">
            {{ t('applications.byUser', { id: d.decidedBy ?? '-' }) }} · {{ d.decidedAt || '' }}
          </div>
        </li>
      </ul>
    </el-tab-pane>

    <el-tab-pane
      :label="t('applications.events')"
      name="events"
    >
      <p
        v-if="!detail.events.length"
        class="ad-empty"
      >
        {{ t('applications.noEvents') }}
      </p>
      <el-timeline v-else>
        <el-timeline-item
          v-for="ev in detail.events"
          :key="ev.id"
          :timestamp="ev.at || ''"
          placement="top"
        >
          <b>{{ ev.eventType }}</b>
          <div class="ad-meta-line">
            {{ t('applications.byUser', { id: ev.actorUserId ?? '-' }) }}
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>
.ad-empty { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
.ad-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.ad-row { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.ad-meta-line { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
.ad-task-actions { display: flex; gap: 8px; margin-top: 6px; }
</style>
