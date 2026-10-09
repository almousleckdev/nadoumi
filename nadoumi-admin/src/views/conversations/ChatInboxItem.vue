<template>
  <li>
    <button
      type="button"
      class="ci"
      :class="{ 'ci--on': active }"
      :aria-current="active ? 'true' : undefined"
      data-test="inbox-item"
      @click="emit('select', conversation.id)"
    >
      <ChatAvatar
        :name="conversation.peer?.name"
        :src="conversation.peer?.avatarUrl"
        :size="44"
        :online="conversation.peer ? conversation.peer.online : null"
      />
      <span class="ci__main">
        <span class="ci__row">
          <span
            class="ci__name"
            :class="{ 'ci__name--unread': unread }"
          >{{ conversation.peer?.name ?? t('conversations.untitled') }}</span>
          <time
            class="ci__time"
            :class="{ 'ci__time--unread': unread }"
            :datetime="conversation.lastMessageAt ?? undefined"
          >{{ inboxTime(conversation.lastMessageAt, locale) }}</time>
        </span>
        <span class="ci__row">
          <span
            class="ci__preview"
            :class="{ 'ci__preview--unread': unread }"
          >
            <ReceiptTicks
              v-if="lastIsMine && status"
              :status="status"
            />
            <span
              class="ci__text"
              dir="auto"
            ><span v-if="lastIsMine">{{ t('conversations.you') }}: </span>{{ preview }}</span>
          </span>
          <span
            v-if="unread"
            class="ci__badge"
            :aria-label="t('conversations.unread', { count: unread })"
            data-test="unread"
          >{{ unread > 99 ? '99+' : unread }}</span>
        </span>
        <span
          v-if="conversation.applicationId"
          class="ci__app"
        >{{ t('conversations.applicationTag', { id: conversation.applicationId }) }}</span>
      </span>
    </button>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChatConversation, ChatMessage } from '@/api/conversation'
import { inboxTime, receiptStatus } from '@/utils/chat'
import ChatAvatar from './ChatAvatar.vue'
import ReceiptTicks from './ReceiptTicks.vue'

const props = defineProps<{ conversation: ChatConversation, active: boolean, myId: number }>()
const emit = defineEmits<{ select: [id: number] }>()
const { t, locale } = useI18n()

const ATTACHMENT_MARK = '📎'
const unread = computed(() => props.conversation.unreadCount)
const lastIsMine = computed(() => props.conversation.lastSenderUserId === props.myId)
const preview = computed(() => {
  const raw = props.conversation.lastMessagePreview
  if (!raw) return t('conversations.noMessagesYet')
  return raw.startsWith(ATTACHMENT_MARK) ? `${ATTACHMENT_MARK} ${t('conversations.attachment')}` : raw
})
const status = computed(() => props.conversation.lastMessageId == null
  ? null
  : receiptStatus({ id: props.conversation.lastMessageId, sendState: 'sent' } as ChatMessage, props.conversation))
</script>

<style scoped>
.ci { box-sizing: border-box; display: flex; align-items: center; gap: 12px; width: 100%; padding: 10px 12px; border: 0; border-inline-start: 4px solid transparent; background: transparent; text-align: start; cursor: pointer; transition: background 0.12s; }
.ci:hover, .ci:focus-visible { background: #f8fafc; outline: none; }
.ci--on, .ci--on:hover { border-inline-start-color: var(--nad-brand-600); background: var(--nad-brand-50); }
.ci__main { display: grid; min-width: 0; flex: 1; gap: 2px; }
.ci__row { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.ci__name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; font-weight: 600; color: var(--nad-ink); }
.ci__name--unread { font-weight: 700; }
.ci__time { flex-shrink: 0; font-size: 12px; color: var(--nad-ink-soft); }
.ci__time--unread { font-weight: 600; color: var(--nad-brand-700); }
.ci__preview { display: flex; flex: 1; align-items: center; gap: 4px; min-width: 0; font-size: 13px; color: var(--nad-ink-soft); }
.ci__preview--unread { font-weight: 500; color: var(--nad-ink); }
.ci__text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci__badge { display: grid; place-items: center; flex-shrink: 0; min-width: 20px; height: 20px; padding: 0 6px; border-radius: 999px; background: var(--nad-brand-700); color: #fff; font-size: 11px; font-weight: 700; }
.ci__app { font-size: 11px; color: var(--nad-ink-soft); }
</style>
