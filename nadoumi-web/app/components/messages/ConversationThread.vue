<script setup lang="ts">
import type { ConversationMessage, ConversationSummary } from '~/types/messages'
import { formatMessageTime } from '~/utils/messages'

const props = defineProps<{
  conversation: ConversationSummary | null
  messages: ConversationMessage[]
  pending: boolean
  error: string
  hasOlder: boolean
  loadingOlder: boolean
  olderError: string
  sending: boolean
  sendError: string
  currentUserId: number | null | undefined
}>()

const emit = defineEmits<{
  back: []
  retry: []
  create: []
  loadOlder: []
  send: [attachmentMediaIds: number[]]
}>()

const draft = defineModel<string>('draft', { required: true })

const { t, locale } = useI18n()

const scroller = ref<HTMLElement | null>(null)
const isClosed = computed(() => props.conversation?.status === 'CLOSED')
const isMine = (m: ConversationMessage) => m.senderUserId === props.currentUserId
const formatTime = (iso: string | null) => (iso ? formatMessageTime(iso, locale.value) : '')

function scrollToEnd() {
  nextTick(() => {
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
  })
}

defineExpose({ scrollToEnd })
</script>

<template>
<section
  class="flex-1 flex flex-col bg-white overflow-hidden"
  :class="{ 'hidden md:flex': conversation === null }"
>
  <!-- Unselected Placeholder State -->
  <div v-if="!conversation" class="flex-1 flex flex-col items-center justify-center p-8 text-center text-slate-500">
    <div class="h-16 w-16 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center mb-4">
      <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" />
      </svg>
    </div>
    <h3 class="font-display text-lg font-semibold text-slate-800">{{ t('dashboard.messages.startConversation') }}</h3>
    <p class="mt-1 max-w-sm text-sm text-slate-500">{{ t('dashboard.messages.startConversationDesc') }}</p>
    <NButton class="mt-6" size="sm" @click="emit('create')">
      {{ t('dashboard.messages.newMessage') }}
    </NButton>
  </div>

  <!-- Selected Thread Content -->
  <template v-else>
    <!-- Thread Header -->
    <header class="flex items-center justify-between px-5 py-3.5 border-b border-slate-200 bg-white">
      <div class="flex items-center gap-3 min-w-0">
        <button
          type="button"
          class="md:hidden -ms-2 p-1.5 text-slate-500 hover:text-slate-800 rounded-lg hover:bg-slate-100"
          :aria-label="t('dashboard.messages.back')"
          @click="emit('back')"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="m15 18-6-6 6-6" />
          </svg>
        </button>
        <div class="h-10 w-10 rounded-full bg-brand-100 text-brand-700 flex items-center justify-center font-bold text-sm shrink-0">
          {{ (conversation.adminName || 'ND').substring(0, 2).toUpperCase() }}
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-2">
            <h2 class="font-semibold text-sm text-slate-900 truncate">
              {{ conversation.subject || t('dashboard.messages.untitled') }}
            </h2>
            <span class="rounded bg-brand-50 text-brand-700 px-1.5 py-0.2 text-[10px] font-medium border border-brand-200/60">
              {{ conversation.adminName || t('dashboard.messages.defaultAdvisor') }}
            </span>
          </div>
          <div class="flex items-center gap-2 text-xs text-slate-500">
            <span v-if="conversation.applicationId" class="text-brand-600 font-medium">
              {{ t('dashboard.messages.applicationRef', { id: conversation.applicationId }) }}
            </span>
            <span v-if="conversation.applicationId && isClosed">•</span>
            <NBadge v-if="isClosed" size="xs">{{ t('dashboard.messages.closedBadge') }}</NBadge>
          </div>
        </div>
      </div>
    </header>

    <!-- Thread Message List -->
    <div ref="scroller" class="flex-1 overflow-y-auto p-4 md:p-6 space-y-4" data-test="thread">
      <div v-if="hasOlder" class="text-center pb-2">
        <NButton variant="ghost" size="sm" :loading="loadingOlder" data-test="load-older" @click="emit('loadOlder')">
          {{ t('dashboard.messages.loadOlder') }}
        </NButton>
        <p v-if="olderError" class="mt-1 text-xs text-red-600" role="alert">{{ olderError }}</p>
      </div>

      <NAlert v-if="error" tone="danger" data-test="thread-error">
        {{ error }}
        <button type="button" class="ms-2 font-medium underline" @click="emit('retry')">
          {{ t('common.retry') }}
        </button>
      </NAlert>

      <p v-else-if="!messages.length && !pending" class="py-8 text-center text-sm text-slate-500">
        {{ t('dashboard.messages.threadEmpty') }}
      </p>

      <div
        v-for="m in messages"
        :key="m.id"
        class="flex"
        :class="isMine(m) ? 'justify-end' : 'justify-start'"
        data-test="message"
      >
        <div
          class="max-w-[85%] rounded-2xl px-4 py-2.5 text-sm shadow-xs"
          :class="isMine(m) ? 'bg-brand-600 text-white rounded-br-sm' : 'bg-slate-100 text-slate-800 rounded-bl-sm border border-slate-200/60'"
        >
          <div class="flex items-center justify-between gap-3 text-xs mb-1" :class="isMine(m) ? 'text-white/80' : 'text-slate-500'">
            <span class="font-semibold">{{ isMine(m) ? t('dashboard.messages.you') : (m.senderName || conversation.adminName || t('dashboard.messages.adminBadge')) }}</span>
            <span class="text-[10px] opacity-70">{{ formatTime(m.createdAt) }}</span>
          </div>
          <p v-if="m.body" class="whitespace-pre-wrap break-words leading-relaxed">{{ m.body }}</p>
          <div v-if="m.attachments && m.attachments.length" class="mt-2 grid gap-1.5">
            <MessageAttachmentView v-for="a in m.attachments" :key="a.id" :conversation-id="conversation.id" :attachment="a" />
          </div>
        </div>
      </div>
    </div>

    <!-- Thread Composer -->
    <div class="p-4 border-t border-slate-200 bg-white">
      <p v-if="isClosed" class="text-sm text-slate-500 py-1" data-test="closed">{{ t('dashboard.messages.closedNotice') }}</p>
      <template v-else>
        <NAlert v-if="sendError" tone="danger" class="mb-3">{{ sendError }}</NAlert>
        <MessageComposer id="thread-composer" v-model="draft" :conversation-id="conversation.id" :busy="sending" @submit="(ids: number[]) => emit('send', ids)" />
      </template>
    </div>
  </template>
</section>
</template>
