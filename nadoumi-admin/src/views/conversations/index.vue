<template>
  <div class="nad-page">
    <PageHeader
      :title="t('conversations.title')"
      :subtitle="t('conversations.subtitle')"
    >
      <template #actions>
        <el-tag
          v-if="reconnecting"
          type="warning"
          effect="plain"
        >
          {{ t('conversations.reconnecting') }}
        </el-tag>
      </template>
    </PageHeader>

    <div class="conv">
      <aside class="conv__list">
        <LoadingState
          v-if="inboxLoading"
          :rows="6"
        />
        <ErrorState
          v-else-if="inboxError"
          :message="inboxError"
          @retry="loadInbox()"
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
              @click="select(c.id)"
            >
              <span class="conv__row-head">
                <span
                  class="conv__row-title"
                  :class="{ 'conv__row-title--unread': c.unreadCount > 0 }"
                >{{ c.subject || t('conversations.untitled') }}</span>
                <el-badge
                  v-if="c.unreadCount > 0"
                  :value="c.unreadCount"
                  type="primary"
                />
              </span>
              <span
                v-if="c.lastMessagePreview"
                class="conv__row-preview"
              >{{ c.lastMessagePreview }}</span>
              <span class="conv__row-meta">
                <el-tag
                  v-if="c.status === 'CLOSED'"
                  size="small"
                  type="info"
                >{{ t('conversations.closed') }}</el-tag>
                <span v-if="c.lastMessageAt">{{ formatTime(c.lastMessageAt) }}</span>
              </span>
            </button>
          </li>
        </ul>
      </aside>

      <section class="conv__thread">
        <EmptyState
          v-if="!selected"
          :title="t('conversations.pickTitle')"
          :description="t('conversations.pickDesc')"
          icon="ChatDotRound"
        />
        <template v-else>
          <header class="conv__thread-head">
            <div>
              <h2 class="conv__thread-title">
                {{ selected.subject || t('conversations.untitled') }}
              </h2>
              <el-tag
                v-if="isClosed"
                size="small"
                type="info"
              >
                {{ t('conversations.closed') }}
              </el-tag>
            </div>
            <el-button
              v-if="isParticipant && !isClosed"
              size="small"
              data-test="close"
              @click="onClose"
            >
              {{ t('conversations.close') }}
            </el-button>
          </header>

          <LoadingState
            v-if="threadLoading"
            :rows="6"
          />
          <ErrorState
            v-else-if="threadError"
            :message="threadError"
            @retry="loadThread()"
          />

          <div
            v-else-if="notParticipant"
            class="conv__join"
            data-test="join-panel"
          >
            <p>{{ t('conversations.notParticipant') }}</p>
            <el-button
              v-if="canManage"
              type="primary"
              :loading="joining"
              data-test="join"
              @click="onJoin"
            >
              {{ t('conversations.join') }}
            </el-button>
            <p
              v-else
              class="conv__hint"
              data-test="join-denied"
            >
              {{ t('conversations.joinDenied') }}
            </p>
          </div>

          <template v-else>
            <div
              ref="scroller"
              class="conv__messages"
              data-test="thread"
            >
              <div
                v-if="hasOlder"
                class="conv__older"
              >
                <el-button
                  size="small"
                  text
                  :loading="loadingOlder"
                  data-test="load-older"
                  @click="loadOlder"
                >
                  {{ t('conversations.loadOlder') }}
                </el-button>
              </div>
              <p
                v-if="!messages.length"
                class="conv__hint"
              >
                {{ t('conversations.threadEmpty') }}
              </p>
              <div
                v-for="m in messages"
                :key="m.id"
                class="conv__msg"
                :class="{ 'conv__msg--mine': isMine(m) }"
                data-test="message"
              >
                <div class="conv__bubble">
                  <p class="conv__sender">
                    {{ isMine(m) ? t('conversations.you') : (m.senderName || `#${m.senderUserId}`) }}
                  </p>
                  <p class="conv__body">
                    {{ m.body }}
                  </p>
                  <ul
                    v-if="m.attachments.length"
                    class="conv__attach"
                  >
                    <li
                      v-for="a in m.attachments"
                      :key="a.id"
                    >
                      {{ t('conversations.attachment') }}: {{ a.filename }}
                    </li>
                  </ul>
                  <p class="conv__time">
                    {{ formatTime(m.createdAt) }}
                  </p>
                </div>
              </div>
            </div>

            <footer class="conv__composer">
              <p
                v-if="isClosed"
                class="conv__hint"
                data-test="closed-notice"
              >
                {{ t('conversations.closedNotice') }}
              </p>
              <form
                v-else
                @submit.prevent="onSend"
              >
                <el-input
                  v-model="draft"
                  type="textarea"
                  :rows="3"
                  :maxlength="MESSAGE_MAX_LENGTH"
                  :placeholder="t('conversations.composer')"
                  data-test="composer"
                  @keydown.ctrl.enter.prevent="onSend"
                  @keydown.meta.enter.prevent="onSend"
                />
                <div class="conv__send">
                  <el-button
                    type="primary"
                    native-type="submit"
                    :loading="sending"
                    :disabled="!draft.trim()"
                    data-test="send"
                  >
                    {{ t('conversations.send') }}
                  </el-button>
                </div>
              </form>
            </footer>
          </template>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import {
  addParticipant, closeConversation, listInbox, listMessages, markConversationRead, postMessage,
  MESSAGE_MAX_LENGTH, MESSAGE_PAGE_SIZE,
  type ConversationMessage, type ConversationSummary,
} from '@/api/conversation'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import { useStaffStream } from '@/composables/useStaffStream'
import { formatMessageTime, mergeMessages } from '@/utils/messages'

const HTTP_FORBIDDEN = 403

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const canManage = computed(() => userStore.hasPerm('nad:conversation:participant:manage'))

// ---- inbox ----
const inbox = ref<ConversationSummary[]>([])
const inboxLoading = ref(true)
const inboxError = ref('')

async function loadInbox(silent = false) {
  if (!silent) inboxLoading.value = true
  inboxError.value = ''
  try {
    inbox.value = await listInbox()
  }
  catch {
    if (!silent) inboxError.value = t('conversations.loadError')
  }
  finally {
    inboxLoading.value = false
  }
}

// ---- selected thread ----
const selectedId = ref<number | null>(Number(route.query.id) || null)
const selected = computed(() => inbox.value.find(c => c.id === selectedId.value) ?? null)
const isClosed = computed(() => selected.value?.status === 'CLOSED')

const messages = ref<ConversationMessage[]>([])
const hasOlder = ref(false)
const threadLoading = ref(false)
const threadError = ref('')
const notParticipant = ref(false)
const isParticipant = computed(() => Boolean(selected.value) && !notParticipant.value && !threadLoading.value && !threadError.value)
const loadingOlder = ref(false)
const scroller = ref<HTMLElement | null>(null)

function scrollToEnd() {
  nextTick(() => {
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
  })
}

function acknowledge(id: number) {
  const row = inbox.value.find(c => c.id === id)
  if (!row?.unreadCount) return
  // Best effort: a badge that lingers is harmless, a failed thread load is not.
  markConversationRead(id)
    .then(() => { row.unreadCount = 0 })
    .catch(() => undefined)
}

function statusOf(e: unknown): number | undefined {
  const err = e as { response?: { status?: number } }
  return err.response?.status
}

async function loadThread() {
  const id = selectedId.value
  if (id === null) return
  messages.value = []
  hasOlder.value = false
  threadError.value = ''
  notParticipant.value = false
  threadLoading.value = true
  try {
    const page = await listMessages(id, 0, true)
    messages.value = mergeMessages([], page)
    hasOlder.value = page.length === MESSAGE_PAGE_SIZE
    acknowledge(id)
    scrollToEnd()
  }
  catch (e) {
    // An unclaimed conversation is listed in the inbox but its thread is only readable once joined.
    if (statusOf(e) === HTTP_FORBIDDEN) notParticipant.value = true
    else threadError.value = t('conversations.loadError')
  }
  finally {
    threadLoading.value = false
  }
}

function select(id: number) {
  if (id === selectedId.value) return
  selectedId.value = id
  router.replace({ query: { ...route.query, id: String(id) } })
  draft.value = ''
  void loadThread()
}

async function syncLatest() {
  const id = selectedId.value
  if (id === null || notParticipant.value || threadError.value) return
  try {
    const before = messages.value.length
    messages.value = mergeMessages(messages.value, await listMessages(id, 0, true))
    if (messages.value.length !== before) {
      acknowledge(id)
      scrollToEnd()
    }
  }
  catch {
    // the next ping or resync retries; a transient failure must not replace the thread with an error
  }
}

async function loadOlder() {
  const oldest = messages.value[0]
  const id = selectedId.value
  if (!oldest || id === null || loadingOlder.value) return
  loadingOlder.value = true
  try {
    const page = await listMessages(id, oldest.id)
    messages.value = mergeMessages(messages.value, page)
    hasOlder.value = page.length === MESSAGE_PAGE_SIZE
  }
  finally {
    loadingOlder.value = false
  }
}

// ---- actions ----
const draft = ref('')
const sending = ref(false)
const joining = ref(false)

async function onSend() {
  const id = selectedId.value
  const body = draft.value.trim()
  if (id === null || !body || sending.value) return
  sending.value = true
  try {
    const sent = await postMessage(id, body)
    draft.value = ''
    messages.value = mergeMessages(messages.value, [sent])
    scrollToEnd()
    void loadInbox(true)
  }
  finally {
    sending.value = false
  }
}

async function onJoin() {
  const id = selectedId.value
  if (id === null || userStore.userId === null) return
  joining.value = true
  try {
    await addParticipant(id, { userId: userStore.userId, role: 'STAFF' })
    ElMessage.success(t('conversations.joined'))
    await loadThread()
    void loadInbox(true)
  }
  finally {
    joining.value = false
  }
}

async function onClose() {
  const id = selectedId.value
  if (id === null) return
  const ok = await confirm({
    title: t('conversations.close'),
    message: t('conversations.closeConfirm'),
    confirmText: t('conversations.close'),
    cancelText: t('common.cancel'),
    tone: 'danger',
  })
  if (!ok) return
  await closeConversation(id)
  ElMessage.success(t('conversations.closedOk'))
  await loadInbox(true)
}

// ---- realtime ----
const { reconnecting } = useStaffStream(
  (refId) => {
    void loadInbox(true)
    if (refId === selectedId.value) void syncLatest()
  },
  () => {
    void loadInbox(true)
    void syncLatest()
  },
)

const isMine = (m: ConversationMessage) => m.senderUserId === userStore.userId
const formatTime = (iso: string) => formatMessageTime(iso, locale.value)

onMounted(async () => {
  await loadInbox()
  if (selectedId.value !== null) await loadThread()
})
</script>

<style scoped>
.conv {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.conv__list,
.conv__thread {
  background: var(--nad-surface, #fff);
  border: 1px solid var(--nad-line, #e5e7eb);
  border-radius: 12px;
  min-height: 420px;
}
.conv__list { padding: 8px; }
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
.conv__row-preview { font-size: 12px; color: var(--nad-ink-soft, #64748b); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__row-meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--nad-ink-faint, #9ca3af); }
.conv__thread { display: flex; flex-direction: column; }
.conv__thread-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px; border-bottom: 1px solid var(--nad-line, #e5e7eb); }
.conv__thread-title { margin: 0 8px 0 0; display: inline; font-size: 16px; }
.conv__messages { display: grid; gap: 10px; align-content: start; max-height: 460px; min-height: 240px; overflow-y: auto; padding: 16px; }
.conv__older { text-align: center; }
.conv__msg { display: flex; justify-content: flex-start; }
.conv__msg--mine { justify-content: flex-end; }
.conv__bubble { max-width: 80%; padding: 8px 12px; border-radius: 12px; background: var(--nad-surface-2, #f1f5f9); font-size: 14px; }
.conv__msg--mine .conv__bubble { background: var(--el-color-primary-light-9, #eef4ff); }
.conv__sender { margin: 0; font-size: 12px; font-weight: 600; color: var(--nad-ink-soft, #64748b); }
.conv__body { margin: 2px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
.conv__attach { margin: 4px 0 0; padding: 0; list-style: none; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.conv__time { margin: 4px 0 0; text-align: end; font-size: 11px; color: var(--nad-ink-faint, #9ca3af); }
.conv__composer { padding: 12px 16px; border-top: 1px solid var(--nad-line, #e5e7eb); margin-top: auto; }
.conv__send { display: flex; justify-content: flex-end; margin-top: 8px; }
.conv__join { display: grid; gap: 12px; justify-items: center; padding: 48px 24px; text-align: center; }
.conv__hint { margin: 0; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
@media (max-width: 900px) {
  .conv { grid-template-columns: 1fr; }
}
</style>
