<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { ApplicationDetail } from '@/api/application'
import { useApplicationLabels } from '../useApplicationLabels'

const TASK_TONES = { OPEN: 'warning', DONE: 'success', SKIPPED: 'neutral', CANCELLED: 'neutral' } as const

defineProps<{ detail: ApplicationDetail, canWork: boolean }>()
const emit = defineEmits<{ complete: [taskId: number], skip: [taskId: number] }>()

const { t } = useI18n()
const { taskStatusLabel } = useApplicationLabels()
</script>

<template>
  <div>
    <p class="tp-help">
      {{ t('applications.tasksHelp') }}
    </p>
    <p
      v-if="!detail.tasks.length"
      class="tp-help"
    >
      {{ t('applications.noTasks') }}
    </p>
    <ul
      v-else
      class="tp-list"
    >
      <li
        v-for="task in detail.tasks"
        :key="task.id"
      >
        <div class="tp-row">
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
          class="tp-sub"
        >
          {{ t('applications.skipReason', { reason: task.skipReason }) }}
        </div>
        <div
          v-if="canWork && task.status === 'OPEN'"
          class="tp-actions"
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
  </div>
</template>

<style scoped>
.tp-help { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.tp-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.tp-row { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.tp-sub { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
.tp-actions { display: flex; gap: 8px; margin-top: 6px; }
</style>
