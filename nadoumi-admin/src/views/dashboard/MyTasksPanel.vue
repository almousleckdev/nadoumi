<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { Task } from '@/api/hr'
import EmptyState from '@/components/ui/EmptyState.vue'

defineProps<{ tasks: Task[], loading: boolean, today: string }>()

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <el-card
    shadow="never"
    class="mt"
  >
    <template #header>
      <span class="mt__title">{{ t('dashboard.myTasks') }}</span>
      <el-button
        link
        type="primary"
        @click="router.push('/tasks')"
      >
        {{ t('dashboard.viewAll') }}
      </el-button>
    </template>
    <el-skeleton
      v-if="loading && !tasks.length"
      :rows="3"
      animated
    />
    <EmptyState
      v-else-if="!tasks.length"
      :title="t('dashboard.noOpenTasks')"
      icon="Select"
    />
    <ul
      v-else
      class="mt__list"
    >
      <li
        v-for="task in tasks"
        :key="task.id"
        class="mt__row"
        data-test="task-row"
        @click="router.push('/tasks')"
      >
        <span
          class="mt__dot"
          :class="`mt__dot--${task.priority}`"
          :title="t(`tasks.priorityMap.${task.priority}`)"
        />
        <span class="mt__text">{{ task.title }}</span>
        <span
          v-if="task.dueDate"
          class="mt__due"
          :class="{ 'mt__due--late': task.dueDate < today }"
        >{{ task.dueDate }}</span>
      </li>
    </ul>
  </el-card>
</template>

<style scoped>
.mt { border: 1px solid var(--nad-line); border-radius: var(--nad-radius); }
.mt :deep(.el-card__header) { display: flex; align-items: center; justify-content: space-between; }
.mt__title { font-weight: 650; font-size: 14px; color: var(--nad-ink); }
.mt__list { list-style: none; margin: 0; padding: 0; }
.mt__row { display: flex; align-items: center; gap: 10px; padding: 9px 0; border-bottom: 1px solid var(--nad-line, #e5e7eb); font-size: 13px; cursor: pointer; }
.mt__row:last-child { border-bottom: 0; }
.mt__dot { width: 8px; height: 8px; border-radius: 50%; flex: none; background: #94a3b8; }
.mt__dot--HIGH { background: var(--el-color-danger, #dc2626); }
.mt__dot--MEDIUM { background: var(--el-color-warning, #d97706); }
.mt__text { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mt__due { font-size: 12px; color: var(--nad-ink-soft, #64748b); font-variant-numeric: tabular-nums; }
.mt__due--late { color: var(--el-color-danger, #dc2626); font-weight: 600; }
</style>
