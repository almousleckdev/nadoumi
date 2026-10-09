<script setup lang="ts">
import { computed } from 'vue'
import type { ChatConversation } from '~/types/chat'
import { inboxTime, receiptStatus } from '~/utils/chat'

const props = defineProps<{ conversation: ChatConversation, active: boolean, myId: number }>()
const emit = defineEmits<{ select: [id: number] }>()
const { t, locale } = useI18n()

const ATTACHMENT_MARK = '📎'
const unread = computed(() => props.conversation.unreadCount)
const lastIsMine = computed(() => props.conversation.lastSenderUserId === props.myId)
const preview = computed(() => {
  const raw = props.conversation.lastMessagePreview
  if (!raw) return t('dashboard.messages.noMessagesYet')
  return raw.startsWith(ATTACHMENT_MARK) ? `${ATTACHMENT_MARK} ${t('dashboard.messages.attachment')}` : raw
})
const status = computed(() => props.conversation.lastMessageId == null
  ? null
  : receiptStatus({ id: props.conversation.lastMessageId, sendState: 'sent' } as never, props.conversation))
</script>

<template>
  <li>
    <button
      type="button"
      class="flex w-full items-center gap-3 border-s-4 px-3 py-3 text-start transition-colors hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline-none"
      :class="active ? 'border-brand-600 bg-brand-50 hover:bg-brand-50' : 'border-transparent'"
      :aria-current="active ? 'true' : undefined"
      data-test="inbox-item"
      @click="emit('select', conversation.id)"
    >
      <ChatAvatar :name="conversation.peer?.name" :src="conversation.peer?.avatarUrl" size="lg" :online="conversation.peer ? conversation.peer.online : null" />
      <span class="min-w-0 flex-1">
        <span class="flex items-baseline justify-between gap-2">
          <span class="truncate text-sm text-slate-900" :class="unread ? 'font-bold' : 'font-semibold'">
            {{ conversation.peer?.name ?? t('dashboard.messages.untitled') }}
          </span>
          <time class="shrink-0 text-xs" :class="unread ? 'font-semibold text-brand-700' : 'text-slate-500'" :datetime="conversation.lastMessageAt ?? undefined">
            {{ inboxTime(conversation.lastMessageAt, locale) }}
          </time>
        </span>
        <span class="mt-0.5 flex items-center justify-between gap-2">
          <span class="flex min-w-0 items-center gap-1 text-sm" :class="unread ? 'font-medium text-slate-900' : 'text-slate-600'">
            <ChatReceiptTicks v-if="lastIsMine && status" :status="status" class="shrink-0" />
            <span class="truncate" dir="auto">
              <span v-if="lastIsMine" class="text-slate-500">{{ t('dashboard.messages.you') }}: </span>{{ preview }}
            </span>
          </span>
          <span
            v-if="unread"
            class="grid h-5 min-w-5 shrink-0 place-items-center rounded-full bg-brand-700 px-1.5 text-[11px] font-bold text-white"
            :aria-label="t('dashboard.messages.unread', { count: unread })"
            data-test="unread"
          >{{ unread > 99 ? '99+' : unread }}</span>
        </span>
      </span>
    </button>
  </li>
</template>
