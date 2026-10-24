<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ApplicationDetail, ApplicationEvent } from '@/api/application'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ detail: ApplicationDetail }>()
const { t } = useI18n()

const STAGE_EVENT = 'STAGE_'
const DECISION_EVENT = 'DECISION_RECORDED'

/** A readable sentence for an engine event; the raw code never reaches the screen. */
function describe(event: ApplicationEvent): string {
  const type = event.eventType
  if (type === 'APPLICATION_CREATED') return t('applications.event.created')
  if (type === 'STAGE_SUBMIT') return t('applications.event.submitted')
  if (type === 'CASE_ASSIGNED') return t('applications.event.assigned', { name: event.detailText ?? '' })
  if (type === 'TASK_COMPLETED') return t('applications.event.taskDone', { title: event.detailText ?? '' })
  if (type === 'TASK_SKIPPED') return t('applications.event.taskSkipped', { title: event.detailText ?? '' })
  if (type.startsWith(DECISION_EVENT)) return t('applications.event.decision', { outcome: (event.detailText ?? '').toLowerCase().replace(/_/g, ' ') })
  if (type.startsWith(STAGE_EVENT)) return t('applications.event.moved')
  return type
}

const items = computed(() => [...props.detail.events].sort((a, b) => (a.at ?? '').localeCompare(b.at ?? '')))
</script>

<template>
  <div>
    <p class="ap-help">
      {{ t('applications.activityHelp') }}
    </p>
    <p
      v-if="!items.length"
      class="ap-help"
    >
      {{ t('applications.noEvents') }}
    </p>
    <el-timeline v-else>
      <el-timeline-item
        v-for="ev in items"
        :key="ev.id"
        :timestamp="formatDateTime(ev.at)"
        placement="top"
      >
        <b>{{ describe(ev) }}</b>
        <div class="ap-by">
          {{ ev.actorName ?? t('applications.system') }}
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped>
.ap-help { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.ap-by { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-top: 2px; }
</style>
