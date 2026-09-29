<script setup lang="ts">
import { MESSAGE_PAGE_SIZE, type ConversationMessage, type ConversationSummary } from '~/types/messages'
import { mergeMessages } from '~/utils/messages'
import { useConversationStream } from '~/composables/useConversationStream'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { user } = useSession()
const { listConversations, listMessages, post, markRead } = useMessages()
const { primary } = useMyApplicant()

const thread = ref<{ scrollToEnd: () => void } | null>(null)

const items = ref<ConversationSummary[]>([])
const pending = ref(true)
const error = ref('')
const searchQuery = ref('')
const showNewModal = ref(false)

const selectedId = ref<number | null>(null)
const threadMessages = ref<ConversationMessage[]>([])
const threadPending = ref(false)
const threadError = ref('')
const hasOlder = ref(false)
const loadingOlder = ref(false)
const olderError = ref('')
const draft = ref('')
const { busy: sending, error: sendError, run: runSend } = useAsyncAction()

const selectedConversation = computed(() =>
  selectedId.value ? items.value.find((c: ConversationSummary) => c.id === selectedId.value) ?? null : null,
)

function scrollToEnd() {
  thread.value?.scrollToEnd()
}

function acknowledge(convId: number) {
  markRead(convId).catch(() => undefined)
  const found = items.value.find((c: ConversationSummary) => c.id === convId)
  if (found) found.unreadCount = 0
}

async function loadInbox(silent = false) {
  if (!silent) pending.value = true
  error.value = ''
  try {
    items.value = await listConversations()
    if (!silent && route.query.id) {
      const qId = Number(route.query.id)
      if (items.value.some((c: ConversationSummary) => c.id === qId)) {
        await selectConversation(qId, false)
      }
    }
  }
  catch {
    if (!silent) error.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}

async function loadThread(convId: number) {
  threadPending.value = true
  threadError.value = ''
  try {
    const page = await listMessages(convId)
    threadMessages.value = mergeMessages([], page)
    hasOlder.value = page.length === MESSAGE_PAGE_SIZE
    acknowledge(convId)
    scrollToEnd()
  }
  catch {
    threadError.value = t('errors.loadSection')
  }
  finally {
    threadPending.value = false
  }
}

async function selectConversation(id: number, updateQuery = true) {
  selectedId.value = id
  if (updateQuery) {
    void router.replace({ query: { ...route.query, id } })
  }
  await loadThread(id)
}

function backToList() {
  selectedId.value = null
  void router.replace({ query: { ...route.query, id: undefined } })
}

async function syncThreadLatest(convId: number) {
  try {
    const before = threadMessages.value.length
    threadMessages.value = mergeMessages(threadMessages.value, await listMessages(convId))
    if (threadMessages.value.length !== before) {
      acknowledge(convId)
      scrollToEnd()
    }
  }
  catch {
    // transient failure during background sync
  }
}

const { reconnecting } = useConversationStream(
  (refId) => {
    void loadInbox(true)
    if (selectedId.value && refId === selectedId.value) {
      void syncThreadLatest(selectedId.value)
    }
  },
  () => {
    void loadInbox(true)
    if (selectedId.value) {
      void syncThreadLatest(selectedId.value)
    }
  },
)

async function loadOlder() {
  const oldest = threadMessages.value[0]
  if (!oldest || loadingOlder.value || !selectedId.value) return
  loadingOlder.value = true
  olderError.value = ''
  try {
    const page = await listMessages(selectedId.value, oldest.id)
    threadMessages.value = mergeMessages(threadMessages.value, page)
    hasOlder.value = page.length === MESSAGE_PAGE_SIZE
  }
  catch {
    olderError.value = t('errors.loadSection')
  }
  finally {
    loadingOlder.value = false
  }
}

async function send(attachmentMediaIds: number[]) {
  if (!selectedId.value) return
  const convId = selectedId.value
  const body = draft.value.trim()
  const sent = await runSend(() => post(convId, body, attachmentMediaIds))
  if (!sent) return
  draft.value = ''
  threadMessages.value = mergeMessages(threadMessages.value, [sent])
  const found = items.value.find((c: ConversationSummary) => c.id === convId)
  if (found) {
    found.lastMessagePreview = body || t('dashboard.messages.attachment')
    found.lastMessageAt = sent.createdAt
  }
  scrollToEnd()
}

async function onConversationCreated(conversationId: number) {
  await loadInbox(true)
  await selectConversation(conversationId)
}

await loadInbox()

useSeo(t('dashboard.messages.title'), t('dashboard.messages.blurb'))
</script>

<template>
  <div class="grid gap-4">
        <!-- Top Header -->
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.messages.title') }}</h1>
        <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.messages.blurb') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <NBadge v-if="reconnecting" tone="warning">{{ t('dashboard.messages.reconnecting') }}</NBadge>
        <NButton size="sm" tone="brand" data-test="new-message" @click="showNewModal = true">
          <svg class="h-4 w-4 me-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          {{ t('dashboard.messages.newMessage') }}
        </NButton>
      </div>
    </header>

    <div class="flex h-[calc(100vh-13rem)] min-h-[580px] max-h-[820px] rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
      <ConversationList
        v-model:search="searchQuery"
        :items="items"
        :pending="pending"
        :error="error"
        :selected-id="selectedId"
        :has-applicant="Boolean(primary)"
        @select="selectConversation"
        @retry="loadInbox()"
        @create="showNewModal = true"
      />
      <ConversationThread
        ref="thread"
        v-model:draft="draft"
        :conversation="selectedConversation"
        :messages="threadMessages"
        :pending="threadPending"
        :error="threadError"
        :has-older="hasOlder"
        :loading-older="loadingOlder"
        :older-error="olderError"
        :sending="sending"
        :send-error="sendError"
        :current-user-id="user?.userId"
        @back="backToList"
        @retry="selectedId && loadThread(selectedId)"
        @create="showNewModal = true"
        @load-older="loadOlder"
        @send="send"
      />
    </div>

    <NewConversationModal
      v-model="showNewModal"
      :applicant-id="primary?.id"
      @created="onConversationCreated"
    />
  </div>
</template>
