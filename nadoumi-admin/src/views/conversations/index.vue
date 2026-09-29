<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  addParticipant, closeConversation, listInbox, listMessages, markConversationRead, postMessage,
  MESSAGE_PAGE_SIZE,
  type ConversationMessage, type ConversationSummary,
} from '@/api/conversation'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import { useStaffStream } from '@/composables/useStaffStream'
import { mergeMessages } from '@/utils/messages'
import ConversationList from './ConversationList.vue'
import ConversationThread from './ConversationThread.vue'
import NewConversationDialog from './NewConversationDialog.vue'

const HTTP_FORBIDDEN = 403
const SEARCH_DEBOUNCE_MS = 300

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const canManage = computed(() => userStore.hasPerm('nad:conversation:participant:manage'))
const thread = ref<InstanceType<typeof ConversationThread>>()

const inbox = ref<ConversationSummary[]>([])
const inboxLoading = ref(true)
const inboxError = ref('')
const searchStudent = ref('')
const searchAppId = ref<number | undefined>()
let searchTimer: ReturnType<typeof setTimeout> | null = null

function debouncedSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    void loadInbox()
  }, SEARCH_DEBOUNCE_MS)
}

async function loadInbox(silent = false) {
  if (!silent) inboxLoading.value = true
  inboxError.value = ''
  try {
    inbox.value = await listInbox({
      studentName: searchStudent.value.trim() || undefined,
      applicationId: searchAppId.value || undefined,
    })
  }
  catch {
    if (!silent) inboxError.value = t('conversations.loadError')
  }
  finally {
    inboxLoading.value = false
  }
}

const newConversationOpen = ref(false)

async function onConversationCreated(id: number) {
  await loadInbox()
  select(id)
}

const selectedId = ref<number | null>(Number(route.query.id) || null)
const selected = computed(() => inbox.value.find(c => c.id === selectedId.value) ?? null)

const messages = ref<ConversationMessage[]>([])
const hasOlder = ref(false)
const threadLoading = ref(false)
const threadError = ref('')
const notParticipant = ref(false)
const loadingOlder = ref(false)

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
    thread.value?.scrollToEnd()
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
      thread.value?.scrollToEnd()
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

const sending = ref(false)
const joining = ref(false)

async function onSend(payload: { body: string, attachmentMediaIds: number[] }) {
  const id = selectedId.value
  if (id === null || sending.value) return
  sending.value = true
  try {
    const sent = await postMessage(id, payload.body, payload.attachmentMediaIds)
    thread.value?.resetComposer()
    messages.value = mergeMessages(messages.value, [sent])
    thread.value?.scrollToEnd()
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

onMounted(async () => {
  await loadInbox()
  if (selectedId.value !== null) await loadThread()
})
</script>

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
      <ConversationList
        v-model:student="searchStudent"
        v-model:application-id="searchAppId"
        :inbox="inbox"
        :loading="inboxLoading"
        :error="inboxError"
        :selected-id="selectedId"
        @typing="debouncedSearch"
        @apply="loadInbox()"
        @select="select"
        @retry="loadInbox()"
        @create="newConversationOpen = true"
      />
      <ConversationThread
        ref="thread"
        :selected="selected"
        :messages="messages"
        :has-older="hasOlder"
        :loading-older="loadingOlder"
        :loading="threadLoading"
        :error="threadError"
        :not-participant="notParticipant"
        :can-manage="canManage"
        :joining="joining"
        :sending="sending"
        :current-user-id="userStore.userId"
        @close="onClose"
        @retry="loadThread()"
        @join="onJoin"
        @load-older="loadOlder"
        @send="onSend"
      />
    </div>

    <NewConversationDialog
      v-model="newConversationOpen"
      @created="onConversationCreated"
    />
  </div>
</template>

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
@media (max-width: 900px) {
  .conv { grid-template-columns: 1fr; }
}
</style>
