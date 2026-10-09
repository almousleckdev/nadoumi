<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import type { ChatConversation, ChatMessage } from '~/types/chat'
import type { LoadStatus, OutgoingFile } from '~/composables/useChat'
import { attachmentUrl } from '~/composables/useChatApi'
import { dayLabel, groupByDay, receiptStatus, relativeTime, sameCluster } from '~/utils/chat'

/**
 * The open conversation: header with presence, the message list (date dividers, older history on scroll, a
 * jump-to-latest pill, drag and drop), and the composer. Scroll position is kept when older messages are
 * prepended and follows new ones only while the reader is already at the bottom.
 */
const props = defineProps<{
  conversation: ChatConversation | null
  messages: ChatMessage[]
  status: LoadStatus
  hasOlder: boolean
  loadingOlder: boolean
  olderFailed: boolean
  myId: number
  reconnecting: boolean
}>()
const draft = defineModel<string>('draft', { required: true })
const emit = defineEmits<{
  back: []
  loadOlder: []
  reload: []
  reopen: []
  send: [body: string, mediaIds: number[], files: OutgoingFile[]]
  retry: [id: number]
  discard: [id: number]
}>()
const { t, locale } = useI18n()

const NEAR_BOTTOM_PX = 120
const NEAR_TOP_PX = 80
const CLOCK_TICK_MS = 30_000

const scroller = ref<HTMLElement | null>(null)
const composer = ref<{ addFiles: (files: File[]) => Promise<void>, focus: () => void } | null>(null)
const atBottom = ref(true)
const missedBelow = ref(0)
const dragging = ref(false)
const lightboxStart = ref<number | null>(null)

const now = ref(new Date())
let clock: ReturnType<typeof setInterval> | undefined
onMounted(() => { clock = setInterval(() => { now.value = new Date() }, CLOCK_TICK_MS) })
onBeforeUnmount(() => clock && clearInterval(clock))

const peer = computed(() => props.conversation?.peer ?? null)
const closed = computed(() => props.conversation?.status === 'CLOSED')
const presenceText = computed(() => {
  if (!peer.value) return ''
  if (peer.value.online) return t('dashboard.messages.online')
  if (!peer.value.lastSeenAt) return t('dashboard.messages.offline')
  return t('dashboard.messages.lastSeen', { when: relativeTime(peer.value.lastSeenAt, locale.value, now.value) })
})
const groups = computed(() => groupByDay(props.messages))
/** Every sent image of the conversation, oldest first: what the viewer steps through. */
const gallery = computed(() => props.conversation
  ? props.messages.flatMap(m => m.attachments)
    .filter(a => a.image && a.id > 0)
    .map(a => ({ url: attachmentUrl(props.conversation!.id, a.id), filename: a.filename }))
  : [])
const lastMessage = computed(() => props.messages[props.messages.length - 1])

function startsRun(list: ChatMessage[], index: number): boolean {
  return !sameCluster(list[index - 1], list[index]!)
}

function scrollToEnd(smooth = false) {
  const el = scroller.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
}

function onScroll() {
  const el = scroller.value
  if (!el) return
  atBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < NEAR_BOTTOM_PX
  if (atBottom.value) missedBelow.value = 0
  if (el.scrollTop < NEAR_TOP_PX && props.hasOlder && !props.loadingOlder && !props.olderFailed) emit('loadOlder')
}

watch(() => props.conversation?.id, () => {
  atBottom.value = true
  missedBelow.value = 0
  draftFocus()
})
watch(() => props.status, async (status) => {
  if (status === 'ready') {
    await nextTick()
    scrollToEnd()
  }
})

watch(() => [props.messages[0]?.id, lastMessage.value?.id, props.messages.length] as const, async (cur, old) => {
  const el = scroller.value
  if (!el || props.status !== 'ready') return
  const heightBefore = el.scrollHeight
  const wasAtBottom = atBottom.value
  const prepended = old[0] !== undefined && cur[0] !== old[0] && cur[1] === old[1]
  const appended = cur[1] !== old[1]
  const mine = lastMessage.value?.senderUserId === props.myId
  await nextTick()
  if (prepended) {
    el.scrollTop += el.scrollHeight - heightBefore
  }
  else if (appended && (wasAtBottom || mine)) {
    scrollToEnd(true)
  }
  else if (appended && !mine) {
    missedBelow.value += 1
  }
})

/** Late-loading images change the height; keep the bottom pinned while the reader is there. */
function onMediaLoad() {
  if (atBottom.value) scrollToEnd()
}

function draftFocus() {
  nextTick(() => composer.value?.focus())
}

function openPreview(url: string) {
  const at = gallery.value.findIndex(g => g.url === url)
  if (at >= 0) lightboxStart.value = at
}
function relaySend(body: string, mediaIds: number[], files: OutgoingFile[]) {
  emit('send', body, mediaIds, files)
}

function onDragOver(e: DragEvent) {
  if (closed.value || !e.dataTransfer?.types.includes('Files')) return
  e.preventDefault()
  dragging.value = true
}
function onDrop(e: DragEvent) {
  dragging.value = false
  if (closed.value) return
  e.preventDefault()
  void composer.value?.addFiles(Array.from(e.dataTransfer?.files ?? []))
}

defineExpose({ scrollToEnd })
</script>

<template>
  <section
    class="relative min-h-0 min-w-0 flex-1 flex-col bg-slate-50"
    :aria-label="t('dashboard.messages.conversation')"
    @dragover="onDragOver"
    @dragleave.self="dragging = false"
    @drop="onDrop"
  >
    <template v-if="conversation">
      <header class="flex items-center gap-3 border-b border-slate-200 bg-white px-3 py-2.5 sm:px-4">
        <button
          type="button"
          class="grid h-9 w-9 shrink-0 place-items-center rounded-full text-slate-600 hover:bg-slate-100 md:hidden"
          :aria-label="t('dashboard.messages.back')"
          data-test="back"
          @click="emit('back')"
        >
          <svg viewBox="0 0 24 24" class="h-5 w-5 rtl:-scale-x-100" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="m15 6-6 6 6 6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <ChatAvatar :name="peer?.name" :src="peer?.avatarUrl" size="md" :online="peer ? peer.online : null" />
        <div class="min-w-0 flex-1">
          <h2 class="truncate font-display text-base font-semibold text-slate-900" data-test="peer-name">
            {{ peer?.name ?? t('dashboard.messages.untitled') }}
          </h2>
          <p class="truncate text-xs" :class="peer?.online ? 'text-emerald-700' : 'text-slate-500'" data-test="presence">
            {{ presenceText }}
          </p>
        </div>
      </header>

      <div
        v-if="reconnecting"
        class="flex items-center justify-center gap-2 bg-amber-50 px-3 py-1.5 text-xs font-medium text-amber-800"
        role="status"
        data-test="reconnecting"
      >
        <NSpinner class="h-3 w-3" />{{ t('dashboard.messages.reconnecting') }}
      </div>

      <div class="relative min-h-0 flex-1">
        <div ref="scroller" class="h-full overflow-y-auto px-3 py-4 sm:px-6" data-test="scroller" @scroll.passive="onScroll" @load.capture="onMediaLoad">
          <div v-if="status === 'loading'" class="grid gap-3" aria-busy="true" data-test="thread-loading">
            <NSkeleton class="h-12 w-2/3 rounded-2xl" />
            <NSkeleton class="ms-auto h-10 w-1/2 rounded-2xl" />
            <NSkeleton class="h-16 w-3/5 rounded-2xl" />
            <NSkeleton class="ms-auto h-12 w-2/5 rounded-2xl" />
          </div>

          <div v-else-if="status === 'error'" class="grid place-items-center gap-3 py-16 text-center" data-test="thread-error">
            <p class="text-sm text-slate-700">{{ t('errors.loadSection') }}</p>
            <NButton size="sm" variant="secondary" @click="emit('reload')">{{ t('dashboard.messages.retry') }}</NButton>
          </div>

          <template v-else>
            <div v-if="hasOlder || loadingOlder || olderFailed" class="mb-3 flex justify-center">
              <span v-if="loadingOlder" class="inline-flex items-center gap-2 text-xs text-slate-500"><NSpinner class="h-3 w-3" />{{ t('dashboard.messages.loadingOlder') }}</span>
              <button v-else type="button" class="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50" data-test="load-older" @click="emit('loadOlder')">
                {{ olderFailed ? t('dashboard.messages.retry') : t('dashboard.messages.loadOlder') }}
              </button>
            </div>

            <p v-if="messages.length === 0" class="py-16 text-center text-sm text-slate-500" data-test="thread-empty">
              {{ t('dashboard.messages.threadEmpty', { name: peer?.name ?? '' }) }}
            </p>

            <div v-for="group in groups" :key="group.key">
              <p class="my-3 text-center">
                <span class="inline-block rounded-full bg-white/90 px-3 py-0.5 text-xs font-medium text-slate-600 shadow-sm ring-1 ring-slate-200 backdrop-blur">
                  {{ dayLabel(group.date, locale, now) }}
                </span>
              </p>
              <ul class="grid gap-1" @load.capture.stop="onMediaLoad">
                <ChatBubble
                  v-for="(m, i) in group.messages"
                  :key="m.id"
                  :message="m"
                  :mine="m.senderUserId === myId"
                  :status="receiptStatus(m, conversation)"
                  :conversation-id="conversation.id"
                  :starts-run="startsRun(group.messages, i)"
                  :class="startsRun(group.messages, i) ? 'mt-2' : ''"
                  @preview="openPreview"
                  @retry="emit('retry', $event)"
                  @discard="emit('discard', $event)"
                />
              </ul>
            </div>
          </template>
        </div>

        <button
          v-if="!atBottom && status === 'ready' && messages.length"
          type="button"
          class="absolute bottom-3 end-4 inline-flex items-center gap-1.5 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-md ring-1 ring-slate-200 hover:bg-slate-50"
          data-test="jump-latest"
          @click="scrollToEnd(true)"
        >
          <span v-if="missedBelow" class="grid h-4 min-w-4 place-items-center rounded-full bg-brand-700 px-1 text-[10px] text-white">{{ missedBelow }}</span>
          {{ missedBelow ? t('dashboard.messages.newMessages') : t('dashboard.messages.jumpLatest') }}
        </button>
      </div>

      <div v-if="closed" class="flex flex-wrap items-center justify-center gap-3 border-t border-slate-200 bg-white px-4 py-3 text-sm text-slate-600" data-test="closed">
        {{ t('dashboard.messages.closedNotice') }}
        <NButton size="sm" variant="secondary" @click="emit('reopen')">{{ t('dashboard.messages.reopen') }}</NButton>
      </div>
      <ChatComposer
        v-else
        ref="composer"
        v-model="draft"
        :conversation-id="conversation.id"
        @send="relaySend"
      />

      <div v-if="dragging" class="pointer-events-none absolute inset-0 z-30 grid place-items-center border-2 border-dashed border-brand-500 bg-brand-50/90" data-test="drop-overlay">
        <p class="text-sm font-semibold text-brand-800">{{ t('dashboard.messages.dropFiles') }}</p>
      </div>
    </template>

    <div v-else class="grid flex-1 place-items-center p-8 text-center">
      <div class="max-w-xs">
        <div class="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-full bg-brand-100 text-brand-700" aria-hidden="true">
          <svg viewBox="0 0 24 24" class="h-7 w-7" fill="none" stroke="currentColor" stroke-width="1.7">
            <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12Z" stroke-linejoin="round" />
          </svg>
        </div>
        <h2 class="font-display text-lg font-semibold text-slate-900">{{ t('dashboard.messages.selectConversation') }}</h2>
        <p class="mt-1 text-sm text-slate-600">{{ t('dashboard.messages.selectConversationDesc') }}</p>
      </div>
    </div>

    <ChatImageLightbox v-if="lightboxStart !== null" :images="gallery" :start="lightboxStart" @close="lightboxStart = null" />
  </section>
</template>
