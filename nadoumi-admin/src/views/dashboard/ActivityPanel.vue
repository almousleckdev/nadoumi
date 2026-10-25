<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { NotificationView } from '@/api/notification'
import EmptyState from '@/components/ui/EmptyState.vue'
import { formatDateTime } from '@/utils/date'

defineProps<{ items: NotificationView[], loading: boolean }>()

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <el-card
    shadow="never"
    class="act"
  >
    <template #header>
      <span class="act__title">{{ t('dashboard.recentActivity') }}</span>
      <el-button
        link
        type="primary"
        @click="router.push('/notifications')"
      >
        {{ t('dashboard.viewAll') }}
      </el-button>
    </template>
    <el-skeleton
      v-if="loading && !items.length"
      :rows="3"
      animated
    />
    <EmptyState
      v-else-if="!items.length"
      :title="t('dashboard.noActivity')"
      icon="Bell"
    />
    <ul
      v-else
      class="act__list"
    >
      <li
        v-for="n in items"
        :key="n.id"
        class="act__row"
        :class="{ 'act__row--unread': !n.read }"
        data-test="activity-row"
      >
        <span class="act__text">{{ n.title || n.body }}</span>
        <span class="act__time">{{ formatDateTime(n.createdAt) }}</span>
      </li>
    </ul>
  </el-card>
</template>

<style scoped>
.act { border: 1px solid var(--nad-line); border-radius: var(--nad-radius); }
.act :deep(.el-card__header) { display: flex; align-items: center; justify-content: space-between; }
.act__title { font-weight: 650; font-size: 14px; color: var(--nad-ink); }
.act__list { list-style: none; margin: 0; padding: 0; }
.act__row { display: flex; flex-direction: column; gap: 2px; padding: 9px 0; border-bottom: 1px solid var(--nad-line, #e5e7eb); font-size: 13px; }
.act__row:last-child { border-bottom: 0; }
.act__row--unread .act__text { font-weight: 650; }
.act__time { font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
