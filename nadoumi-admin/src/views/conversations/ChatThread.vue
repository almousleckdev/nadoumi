<template>
  <section
    class="ct"
    :aria-label="t('conversations.conversation')"
    @dragover="onDragOver"
    @dragleave.self="dragging = false"
    @drop="onDrop"
  >
    <template v-if="conversation">
      <header class="ct__head">
        <button
          type="button"
          class="ct__back"
          :aria-label="t('conversations.back')"
          data-test="back"
          @click="emit('back')"
        >
          <el-icon :size="18">
            <ArrowLeft />
          </el-icon>
        </button>
        <ChatAvatar
          :name="peer?.name"
          :src="peer?.avatarUrl"
          :size="40"
          :online="peer ? peer.online : null"
        />
        <div class="ct__who">
          <h2
            class="ct__name"
            data-test="peer-name"
          >
            {{ peer?.name ?? t('conversations.untitled') }}
          </h2>
          <p
            class="ct__presence"
            :class="{ 'ct__presence--on': peer?.online }"
            data-test="presence"
          >
            {{ presenceText }}
          </p>
        </div>
        <el-tag
          v-if="conversation.applicationId"
          effect="plain"
          size="small"
        >
          {{ t('conversations.applicationTag', { id: conversation.applicationId }) }}
        </el-tag>
        <el-button
          v-if="!closed"
          size="small"
          plain
          data-test="close-chat"
          @click="emit('close')"
        >
          {{ t('conversations.close') }}
        </el-button>
      </header>

      <div
        v-if="reconnecting"
        class="ct__banner"
        role="status"
        data-test="reconnecting"
      >
        <el-icon class="is-loading">
          <Loading />
        </el-icon>{{ t('conversations.reconnecting') }}
      </div>

      <div class="ct__body">
        <div
          ref="scroller"
          class="ct__scroll"
          data-test="scroller"
          @scroll.passive="onScroll"
          @load.capture="onMediaLoad"
        >
          <div
            v-if="status === 'loading'"
            class="ct__skeleton"
            aria-busy="true"
            data-test="thread-loading"
          >
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item
                  variant="rect"
                  style="width: 60%; height: 48px; border-radius: 16px"
                />
                <el-skeleton-item
                  variant="rect"
                  style="width: 45%; height: 40px; border-radius: 16px; margin-top: 12px; margin-inline-start: auto; display: block"
                />
                <el-skeleton-item
                  variant="rect"
                  style="width: 55%; height: 56px; border-radius: 16px; margin-top: 12px"
                />
              </template>
            </el-skeleton>
          </div>

          <div
            v-else-if="status === 'error'"
            class="ct__state"
            data-test="thread-error"
          >
            <p>{{ t('conversations.loadError') }}</p>
            <el-button
              size="small"
              @click="emit('reload')"
            >
              {{ t('conversations.retry') }}
            </el-button>
          </div>

          <template v-else>
            <div
              v-if="hasOlder || loadingOlder || olderFailed"
              class="ct__older"
            >
              <span
                v-if="loadingOlder"
                class="ct__older-busy"
              ><el-icon class="is-loading"><Loading /></el-icon>{{ t('conversations.loadingOlder') }}</span>
              <button
                v-else
                type="button"
                class="ct__pill"
                data-test="load-older"
                @click="emit('loadOlder')"
              >
                {{ olderFailed ? t('conversations.retry') : t('conversations.loadOlder') }}
              </button>
            </div>

            <p
              v-if="messages.length === 0"
              class="ct__empty"
              data-test="thread-empty"
            >
              {{ t('conversations.threadEmpty', { name: peer?.name ?? '' }) }}
            </p>

            <div
              v-for="group in groups"
              :key="group.key"
            >
              <p class="ct__day">
                <span>{{ dayLabel(group.date, locale, now) }}</span>
              </p>
              <ul class="ct__list">
                <ChatBubble
                  v-for="(m, i) in group.messages"
                  :key="m.id"
                  :message="m"
                  :mine="m.senderUserId === myId"
                  :status="receiptStatus(m, conversation)"
                  :conversation-id="conversation.id"
                  :starts-run="startsRun(group.messages, i)"
                  :class="{ 'ct__gap': startsRun(group.messages, i) }"
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
          class="ct__jump"
          data-test="jump-latest"
          @click="scrollToEnd(true)"
        >
          <span
            v-if="missedBelow"
            class="ct__jump-n"
          >{{ missedBelow }}</span>
          {{ missedBelow ? t('conversations.newMessages') : t('conversations.jumpLatest') }}
        </button>
      </div>

      <div
        v-if="closed"
        class="ct__closed"
        data-test="closed"
      >
        {{ t('conversations.closedNotice') }}
        <el-button
          size="small"
          @click="emit('reopen')"
        >
          {{ t('conversations.reopen') }}
        </el-button>
      </div>
      <ChatComposer
        v-else
        ref="composer"
        v-model="draft"
        :conversation-id="conversation.id"
        @send="relaySend"
      />

      <div
        v-if="dragging"
        class="ct__drop"
        data-test="drop-overlay"
      >
        {{ t('conversations.dropFiles') }}
      </div>
    </template>

    <div
      v-else
      class="ct__pick"
    >
      <el-icon
        :size="40"
        class="ct__pick-icon"
      >
        <ChatDotRound />
      </el-icon>
      <h2>{{ t('conversations.pickTitle') }}</h2>
      <p>{{ t('conversations.pickDesc') }}</p>
    </div>

    <ImageLightbox
      v-if="lightboxStart !== null"
      :images="gallery"
      :start="lightboxStart"
      @close="lightboxStart = null"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ChatDotRound, Loading } from '@element-plus/icons-vue'
import type { ChatConversation, ChatMessage } from '@/api/conversation'
import type { LoadStatus, OutgoingFile } from '@/composables/useStaffChat'
import { attachmentUrl } from '@/api/conversation'
import { dayLabel, groupByDay, receiptStatus, relativeTime, sameCluster } from '@/utils/chat'
import ChatAvatar from './ChatAvatar.vue'
import ChatBubble from './ChatBubble.vue'
import ChatComposer from './ChatComposer.vue'
import ImageLightbox from './ImageLightbox.vue'

/**
 * The open conversation: header with presence, the message list (date dividers, older history on scroll, a
 * jump-to-latest pill, drag and drop), and the reply box. Scroll position is kept when older messages are
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
  close: []
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
  if (peer.value.online) return t('conversations.online')
  if (!peer.value.lastSeenAt) return t('conversations.offline')
  return t('conversations.lastSeen', { when: relativeTime(peer.value.lastSeenAt, locale.value, now.value) })
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
  nextTick(() => composer.value?.focus())
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

<style scoped>
.ct { position: relative; display: flex; flex-direction: column; min-width: 0; min-height: 0; flex: 1; background: #f8fafc; }
.ct__head { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid var(--nad-line); background: #fff; }
.ct__back { display: none; width: 36px; height: 36px; place-items: center; border: 0; border-radius: 50%; background: transparent; color: var(--nad-ink-soft); cursor: pointer; }
.ct__back:hover { background: #f1f5f9; }
.ct__who { min-width: 0; flex: 1; }
.ct__name { margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 16px; font-weight: 600; color: var(--nad-ink); }
.ct__presence { margin: 0; font-size: 12px; color: var(--nad-ink-soft); }
.ct__presence--on { color: #047857; }
.ct__banner { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 6px 12px; background: #fffbeb; color: #92400e; font-size: 12px; font-weight: 500; }
.ct__body { position: relative; flex: 1; min-height: 0; }
.ct__scroll { height: 100%; padding: 16px 24px; overflow-y: auto; }
.ct__list { display: grid; gap: 4px; margin: 0; padding: 0; }
.ct__gap { margin-top: 8px; }
.ct__day { margin: 12px 0; text-align: center; }
.ct__day span { display: inline-block; padding: 2px 12px; border-radius: 999px; background: rgba(255, 255, 255, 0.92); box-shadow: 0 0 0 1px var(--nad-line); color: var(--nad-ink-soft); font-size: 12px; font-weight: 500; }
.ct__older { display: flex; justify-content: center; margin-bottom: 12px; }
.ct__older-busy { display: inline-flex; align-items: center; gap: 8px; font-size: 12px; color: var(--nad-ink-soft); }
.ct__pill { padding: 4px 12px; border: 1px solid var(--nad-line); border-radius: 999px; background: #fff; color: var(--nad-ink); font-size: 12px; font-weight: 500; cursor: pointer; }
.ct__pill:hover { background: #f8fafc; }
.ct__skeleton { max-width: 520px; }
.ct__state, .ct__empty { display: grid; justify-items: center; gap: 12px; padding: 64px 0; color: var(--nad-ink-soft); font-size: 14px; text-align: center; }
.ct__empty { margin: 0; }
.ct__jump { position: absolute; inset-block-end: 12px; inset-inline-end: 16px; display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border: 0; border-radius: 999px; background: #fff; box-shadow: var(--nad-shadow-md), 0 0 0 1px var(--nad-line); color: var(--nad-ink); font-size: 12px; font-weight: 600; cursor: pointer; }
.ct__jump-n { display: grid; place-items: center; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 999px; background: var(--nad-brand-700); color: #fff; font-size: 10px; }
.ct__closed { display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 12px; padding: 12px 16px; border-top: 1px solid var(--nad-line); background: #fff; color: var(--nad-ink-soft); font-size: 14px; }
.ct__drop { position: absolute; inset: 0; z-index: 30; display: grid; place-items: center; border: 2px dashed var(--nad-brand-500); background: rgba(255, 247, 237, 0.92); color: var(--nad-brand-700); font-weight: 600; pointer-events: none; }
.ct__pick { display: grid; flex: 1; place-content: center; justify-items: center; gap: 4px; padding: 32px; text-align: center; color: var(--nad-ink-soft); }
.ct__pick-icon { padding: 14px; border-radius: 50%; background: var(--nad-brand-100); color: var(--nad-brand-700); box-sizing: content-box; }
.ct__pick h2 { margin: 12px 0 0; font-size: 18px; color: var(--nad-ink); }
.ct__pick p { margin: 0; font-size: 14px; }
@media (max-width: 900px) {
  .ct__back { display: grid; }
  .ct__scroll { padding: 12px; }
}
</style>
