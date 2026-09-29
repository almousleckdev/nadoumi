<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus, Search } from '@element-plus/icons-vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { ConversationSummary } from '@/api/conversation'
import { formatMessageTime } from '@/utils/messages'

defineProps<{
  inbox: ConversationSummary[]
  loading: boolean
  error: string
  selectedId: number | null
}>()

const emit = defineEmits<{
  typing: []
  apply: []
  select: [id: number]
  retry: []
  create: []
}>()

const student = defineModel<string>('student', { required: true })
const applicationId = defineModel<number | undefined>('applicationId', { required: true })

const { t, locale } = useI18n()
</script>

<template>
  <aside class="conv__list">
    <div class="conv__search-bar">
      <el-input
        v-model="student"
        :placeholder="t('conversations.searchPlaceholder')"
        clearable
        size="small"
        :prefix-icon="Search"
        @input="emit('typing')"
        @clear="emit('apply')"
      />
      <div class="conv__search-actions">
        <el-input
          v-model.number="applicationId"
          :placeholder="t('conversations.appIdPlaceholder')"
          clearable
          size="small"
          style="width: 80px"
          @change="emit('apply')"
          @clear="emit('apply')"
        />
        <el-button
          type="primary"
          size="small"
          :icon="Plus"
          @click="emit('create')"
        >
          {{ t('conversations.newConversation') }}
        </el-button>
      </div>
    </div>

    <LoadingState
      v-if="loading"
      :rows="6"
    />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="emit('retry')"
    />
    <EmptyState
      v-else-if="!inbox.length"
      :title="t('conversations.emptyTitle')"
      :description="t('conversations.emptyDesc')"
      icon="ChatDotRound"
    />
    <ul
      v-else
      class="conv__rows"
    >
      <li
        v-for="c in inbox"
        :key="c.id"
      >
        <button
          type="button"
          class="conv__row"
          :class="{ 'conv__row--active': c.id === selectedId }"
          data-test="conversation-row"
          @click="emit('select', c.id)"
        >
          <span class="conv__row-head">
            <span
              class="conv__row-title"
              :class="{ 'conv__row-title--unread': c.unreadCount > 0 }"
            >{{ c.studentName || c.subject || t('conversations.untitled') }}</span>
            <el-badge
              v-if="c.unreadCount > 0"
              :value="c.unreadCount"
              type="primary"
            />
          </span>
          <span
            v-if="c.studentName && c.subject"
            class="conv__row-sub"
          >{{ c.subject }}</span>
          <span
            v-if="c.lastMessagePreview"
            class="conv__row-preview"
          >{{ c.lastMessagePreview }}</span>
          <span class="conv__row-meta">
            <el-tag
              v-if="c.applicationId"
              size="small"
              type="success"
              effect="plain"
            >{{ t('conversations.applicationTag', { id: c.applicationId }) }}</el-tag>
            <el-tag
              v-if="c.status === 'CLOSED'"
              size="small"
              type="info"
            >{{ t('conversations.closed') }}</el-tag>
            <span v-if="c.lastMessageAt">{{ formatMessageTime(c.lastMessageAt, locale) }}</span>
          </span>
        </button>
      </li>
    </ul>
  </aside>
</template>

<style scoped>
.conv__list { padding: 8px; display: flex; flex-direction: column; gap: 8px; }
.conv__search-bar { display: flex; flex-direction: column; gap: 6px; padding: 4px; border-bottom: 1px solid var(--nad-line, #e5e7eb); margin-bottom: 4px; }
.conv__search-actions { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.conv__rows { list-style: none; margin: 0; padding: 0; display: grid; gap: 4px; }
.conv__row {
  width: 100%;
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  text-align: start;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}
.conv__row:hover { background: var(--nad-surface-2, #f8fafc); }
.conv__row--active { background: var(--nad-surface-2, #f1f5f9); }
.conv__row-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.conv__row-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__row-title--unread { font-weight: 700; }
.conv__row-sub { font-size: 12px; color: var(--el-color-primary, #4338ca); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__row-preview { font-size: 12px; color: var(--nad-ink-soft, #64748b); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__row-meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--nad-ink-faint, #9ca3af); }
</style>
