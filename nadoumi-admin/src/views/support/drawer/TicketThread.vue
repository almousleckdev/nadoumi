<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { TicketMessage } from '@/api/support'

defineProps<{ messages: TicketMessage[], openedByUserId: number, canReply: boolean, busy: boolean }>()
const emit = defineEmits<{ send: [] }>()
const draft = defineModel<string>('draft', { required: true })

const { t } = useI18n()
</script>

<template>
  <div>
    <h4 class="st-h4">
      {{ t('support.conversation') }}
    </h4>
    <p
      v-if="!messages.length"
      class="st-muted"
    >
      {{ t('support.noMessages') }}
    </p>
    <ul
      v-else
      class="st-thread"
      data-test="thread"
    >
      <li
        v-for="m in messages"
        :key="m.id"
        class="st-msg"
        :class="{ 'st-msg--student': m.senderUserId === openedByUserId }"
      >
        <div class="st-msg-meta">
          {{ m.senderName || m.senderUserId }} · {{ m.createdAt }}
        </div>
        <div class="st-msg-body">
          {{ m.body }}
        </div>
      </li>
    </ul>

    <div
      v-if="canReply"
      class="st-reply"
      data-test="reply"
    >
      <el-input
        v-model="draft"
        type="textarea"
        :rows="3"
        maxlength="4000"
        :placeholder="t('support.replyPlaceholder')"
      />
      <el-button
        type="primary"
        size="small"
        :disabled="!draft.trim()"
        :loading="busy"
        data-test="send-reply"
        @click="emit('send')"
      >
        {{ t('support.send') }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.st-muted { color: var(--nad-ink-soft, #64748b); font-size: 13px; }
.st-h4 { margin: 0 0 12px; font-size: 13px; }
.st-thread { list-style: none; margin: 0 0 16px; padding: 0; display: grid; gap: 10px; }
.st-msg { border: 1px solid var(--el-border-color-light); border-radius: 8px; padding: 10px 12px; background: #fff; }
.st-msg--student { background: var(--el-fill-color-lighter); }
.st-msg-meta { font-size: 12px; color: var(--nad-ink-soft, #64748b); margin-bottom: 4px; }
.st-msg-body { white-space: pre-wrap; word-break: break-word; font-size: 14px; }
.st-reply { display: grid; gap: 8px; justify-items: start; margin: 0 0 20px; }
.st-reply :deep(.el-textarea) { width: 100%; }
</style>
