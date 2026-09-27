<script setup lang="ts">
import { MESSAGE_PAGE_SIZE, type AdminContact, type ConversationMessage, type ConversationSummary } from '~/types/messages'
import type { StudentApplicationDto } from '~/types/catalog'
import { formatMessageTime, mergeMessages } from '~/utils/messages'
import { useConversationStream } from '~/composables/useConversationStream'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const { user } = useSession()
const { listConversations, listAdmins, listMessages, post, open, markRead } = useMessages()
const { primary } = useMyApplicant()
const { list: listApplications } = useApplications()

// --- Conversation List State ---
const items = ref<ConversationSummary[]>([])
const pending = ref(true)
const error = ref('')
const searchQuery = ref('')

// --- Selected Thread State ---
const selectedId = ref<number | null>(null)
const threadMessages = ref<ConversationMessage[]>([])
const threadPending = ref(false)
const threadError = ref('')
const hasOlder = ref(false)
const loadingOlder = ref(false)
const olderError = ref('')
const draft = ref('')
const scroller = ref<HTMLElement | null>(null)
const { busy: sending, error: sendError, run: runSend } = useAsyncAction()

// --- New Conversation Modal State ---
const showNewModal = ref(false)
const admins = ref<AdminContact[]>([])
const studentApplications = ref<StudentApplicationDto[]>([])
const newAdminUserId = ref<string>('')
const newApplicationId = ref<string>('')
const newSubject = ref('')
const newMessageBody = ref('')
const newSubmitting = ref(false)
const newError = ref('')

const selectedConversation = computed(() =>
  selectedId.value ? items.value.find(c => c.id === selectedId.value) ?? null : null,
)

const isClosed = computed(() => selectedConversation.value?.status === 'CLOSED')

const filteredItems = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((c) => {
    const subject = (c.subject || '').toLowerCase()
    const admin = (c.adminName || '').toLowerCase()
    const preview = (c.lastMessagePreview || '').toLowerCase()
    const appId = c.applicationId ? String(c.applicationId) : ''
    return subject.includes(q) || admin.includes(q) || preview.includes(q) || appId.includes(q)
  })
})

function scrollToEnd() {
  nextTick(() => {
    if (scroller.value) {
      scroller.value.scrollTop = scroller.value.scrollHeight
    }
  })
}

function acknowledge(convId: number) {
  markRead(convId).catch(() => undefined)
  const found = items.value.find(c => c.id === convId)
  if (found) found.unreadCount = 0
}

async function loadInbox(silent = false) {
  if (!silent) pending.value = true
  error.value = ''
  try {
    items.value = await listConversations()
    // If a query param is present on mount, select that conversation
    if (!silent && route.query.id) {
      const qId = Number(route.query.id)
      if (items.value.some(c => c.id === qId)) {
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
  // Update preview in left list
  const found = items.value.find(c => c.id === convId)
  if (found) {
    found.lastMessagePreview = body || t('dashboard.messages.attachment')
    found.lastMessageAt = sent.createdAt
  }
  scrollToEnd()
}

// --- New Conversation Dialog Operations ---
async function openNewModal() {
  newError.value = ''
  newSubject.value = ''
  newMessageBody.value = ''
  newAdminUserId.value = ''
  newApplicationId.value = ''
  showNewModal.value = true

  // Lazily load admins and applications
  try {
    if (!admins.value.length && listAdmins) {
      admins.value = await listAdmins()
    }
    if (!studentApplications.value.length && listApplications) {
      studentApplications.value = await listApplications()
    }
  }
  catch {
    // Best-effort; fallback to default admin and manual input
  }
}

const adminSelectOptions = computed(() => [
  { value: '', label: t('dashboard.messages.defaultAdvisor') },
  ...admins.value.map(a => ({
    value: String(a.userId),
    label: `${a.name}${a.email ? ` (${a.email})` : ''}`,
  })),
])

const applicationSelectOptions = computed(() => [
  { value: '', label: '—' },
  ...studentApplications.value.map(app => ({
    value: String(app.id),
    label: `#${app.id} - ${app.applicationType || 'Application'} (${app.currentStatus || 'Draft'})`,
  })),
])

async function submitNewConversation() {
  if (!newSubject.value.trim() || !newMessageBody.value.trim()) return
  newSubmitting.value = true
  newError.value = ''
  try {
    const adminId = newAdminUserId.value ? Number(newAdminUserId.value) : undefined
    const appId = newApplicationId.value ? Number(newApplicationId.value) : undefined

    const opened = await open({
      applicantId: primary.value?.id,
      applicationId: appId,
      adminUserId: adminId,
      subject: newSubject.value.trim(),
      body: newMessageBody.value.trim(),
    })

    showNewModal.value = false
    await loadInbox(true)
    if (opened && opened.conversationId) {
      await selectConversation(opened.conversationId)
    }
  }
  catch (err: unknown) {
    const message = err instanceof Error ? err.message : t('errors.unexpected')
    newError.value = message
  }
  finally {
    newSubmitting.value = false
  }
}

const isMine = (m: ConversationMessage) => m.senderUserId === user.value?.userId
const formatTime = (iso: string | null) => (iso ? formatMessageTime(iso, locale.value) : '')

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
        <NButton size="sm" tone="brand" data-test="new-message" @click="openNewModal">
          <svg class="h-4 w-4 me-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          {{ t('dashboard.messages.newMessage') }}
        </NButton>
      </div>
    </header>

    <!-- Master-Detail 2-Pane Container -->
    <div class="flex h-[calc(100vh-13rem)] min-h-[580px] max-h-[820px] rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
      <!-- Left Pane: Conversation List -->
      <aside
        class="w-full md:w-80 lg:w-96 flex flex-col border-e border-slate-200 bg-slate-50/60"
        :class="{ 'hidden md:flex': selectedId !== null }"
      >
        <!-- Search Bar -->
        <div class="p-3 border-b border-slate-200 bg-white">
          <NInput
            id="conv-search"
            v-model="searchQuery"
            :placeholder="t('dashboard.messages.searchPlaceholder')"
            aria-label="Search conversations"
          >
            <template #prefix>
              <svg class="h-4 w-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="8" stroke-width="2" />
                <path d="m21 21-4.35-4.35" stroke-width="2" />
              </svg>
            </template>
          </NInput>
        </div>

        <!-- Conversations Content with AsyncState -->
        <AsyncState :pending="pending" :error="error" :empty="!items.length" class="flex-1 flex flex-col overflow-hidden">
          <template #loading>
            <div class="p-3 grid gap-3">
              <NSkeleton v-for="n in 4" :key="n" class="h-16 w-full rounded-xl" />
            </div>
          </template>

          <template #error>
            <div class="p-4">
              <NAlert tone="danger">
                {{ error }}
                <button type="button" class="ms-2 font-medium underline" @click="loadInbox()">
                  {{ t('common.retry') }}
                </button>
              </NAlert>
            </div>
          </template>

          <template #empty>
            <div class="flex-1 flex flex-col items-center justify-center p-6 text-center text-slate-500">
              <div class="h-12 w-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-400 mb-3">
                <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" />
                </svg>
              </div>
              <p v-if="primary" class="text-sm font-medium text-slate-700">{{ t('dashboard.messages.empty') }}</p>
              <p v-else class="text-sm font-medium text-slate-700">{{ t('dashboard.messages.noApplicant') }}</p>
              <NButton v-if="primary" class="mt-4" size="sm" @click="openNewModal">
                {{ t('dashboard.messages.newMessage') }}
              </NButton>
            </div>
          </template>

          <!-- Conversation Rows -->
          <div v-if="filteredItems.length" class="flex-1 overflow-y-auto divide-y divide-slate-100">
            <ul class="divide-y divide-slate-100">
              <li v-for="c in filteredItems" :key="c.id">
                <button
                  type="button"
                  class="w-full text-start p-3.5 transition-colors relative flex items-start gap-3 hover:bg-slate-100/80 cursor-pointer"
                  :class="c.id === selectedId ? 'bg-brand-50/80 border-s-4 border-brand-600' : 'bg-white'"
                  data-test="conversation-row"
                  @click="selectConversation(c.id)"
                >
                  <!-- Avatar -->
                  <div class="relative shrink-0">
                    <div class="h-10 w-10 rounded-full bg-brand-100 text-brand-700 flex items-center justify-center font-bold text-xs">
                      {{ (c.adminName || 'ND').substring(0, 2).toUpperCase() }}
                    </div>
                    <span
                      v-if="c.unreadCount > 0"
                      class="absolute -top-1 -end-1 flex h-4 w-4 items-center justify-center rounded-full bg-brand-600 text-[10px] font-bold text-white shadow-xs"
                    >
                      {{ c.unreadCount }}
                    </span>
                  </div>

                  <!-- Details -->
                  <div class="min-w-0 flex-1">
                    <div class="flex items-center justify-between gap-1 mb-0.5">
                      <div class="flex items-center gap-1.5 truncate">
                        <span class="text-xs font-semibold text-slate-900 truncate">
                          {{ c.adminName || t('dashboard.messages.defaultAdvisor') }}
                        </span>
                        <span class="rounded bg-slate-100 px-1.5 py-0.2 text-[10px] font-medium text-slate-500">
                          Staff
                        </span>
                      </div>
                      <span v-if="c.lastMessageAt" class="shrink-0 text-[11px] text-slate-400">
                        {{ formatTime(c.lastMessageAt) }}
                      </span>
                    </div>

                    <p class="truncate text-xs" :class="c.unreadCount ? 'font-semibold text-slate-900' : 'text-slate-700'">
                      {{ c.subject || t('dashboard.messages.untitled') }}
                    </p>

                    <p v-if="c.lastMessagePreview" class="mt-1 truncate text-xs text-slate-500">
                      {{ c.lastMessagePreview }}
                    </p>

                    <div class="mt-1.5 flex flex-wrap items-center gap-1.5">
                      <NBadge v-if="c.applicationId" size="xs" tone="neutral">#app {{ c.applicationId }}</NBadge>
                      <NBadge v-if="c.status === 'CLOSED'" size="xs">{{ t('dashboard.messages.closedBadge') }}</NBadge>
                      <NBadge v-if="c.unreadCount" size="xs" tone="brand">{{ t('dashboard.messages.unread', { count: c.unreadCount }) }}</NBadge>
                    </div>
                  </div>
                </button>
              </li>
            </ul>
          </div>
          <div v-else-if="items.length" class="p-6 text-center text-xs text-slate-500">
            {{ t('dashboard.messages.noResults') }}
          </div>
        </AsyncState>
      </aside>

      <!-- Right Pane: Active Thread -->
      <section
        class="flex-1 flex flex-col bg-white overflow-hidden"
        :class="{ 'hidden md:flex': selectedId === null }"
      >
        <!-- Unselected Placeholder State -->
        <div v-if="!selectedConversation" class="flex-1 flex flex-col items-center justify-center p-8 text-center text-slate-500">
          <div class="h-16 w-16 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center mb-4">
            <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" />
            </svg>
          </div>
          <h3 class="font-display text-lg font-semibold text-slate-800">{{ t('dashboard.messages.startConversation') }}</h3>
          <p class="mt-1 max-w-sm text-sm text-slate-500">{{ t('dashboard.messages.startConversationDesc') }}</p>
          <NButton class="mt-6" size="sm" @click="openNewModal">
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
                aria-label="Back to conversations"
                @click="backToList"
              >
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="m15 18-6-6 6-6" />
                </svg>
              </button>
              <div class="h-10 w-10 rounded-full bg-brand-100 text-brand-700 flex items-center justify-center font-bold text-sm shrink-0">
                {{ (selectedConversation.adminName || 'ND').substring(0, 2).toUpperCase() }}
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <h2 class="font-semibold text-sm text-slate-900 truncate">
                    {{ selectedConversation.subject || t('dashboard.messages.untitled') }}
                  </h2>
                  <span class="rounded bg-brand-50 text-brand-700 px-1.5 py-0.2 text-[10px] font-medium border border-brand-200/60">
                    {{ selectedConversation.adminName || t('dashboard.messages.defaultAdvisor') }}
                  </span>
                </div>
                <div class="flex items-center gap-2 text-xs text-slate-500">
                  <span v-if="selectedConversation.applicationId" class="text-brand-600 font-medium">
                    Application #{{ selectedConversation.applicationId }}
                  </span>
                  <span v-if="selectedConversation.applicationId && isClosed">•</span>
                  <NBadge v-if="isClosed" size="xs">{{ t('dashboard.messages.closedBadge') }}</NBadge>
                </div>
              </div>
            </div>
          </header>

          <!-- Thread Message List -->
          <div ref="scroller" class="flex-1 overflow-y-auto p-4 md:p-6 space-y-4" data-test="thread">
            <div v-if="hasOlder" class="text-center pb-2">
              <NButton variant="ghost" size="sm" :loading="loadingOlder" data-test="load-older" @click="loadOlder">
                {{ t('dashboard.messages.loadOlder') }}
              </NButton>
              <p v-if="olderError" class="mt-1 text-xs text-red-600" role="alert">{{ olderError }}</p>
            </div>

            <p v-if="!threadMessages.length && !threadPending" class="py-8 text-center text-sm text-slate-500">
              {{ t('dashboard.messages.threadEmpty') }}
            </p>

            <div
              v-for="m in threadMessages"
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
                  <span class="font-semibold">{{ isMine(m) ? t('dashboard.messages.you') : (m.senderName || selectedConversation.adminName || 'Staff Advisor') }}</span>
                  <span class="text-[10px] opacity-70">{{ formatTime(m.createdAt) }}</span>
                </div>
                <p v-if="m.body" class="whitespace-pre-wrap break-words leading-relaxed">{{ m.body }}</p>
                <div v-if="m.attachments && m.attachments.length" class="mt-2 grid gap-1.5">
                  <MessageAttachmentView v-for="a in m.attachments" :key="a.id" :conversation-id="selectedConversation.id" :attachment="a" />
                </div>
              </div>
            </div>
          </div>

          <!-- Thread Composer -->
          <div class="p-4 border-t border-slate-200 bg-white">
            <p v-if="isClosed" class="text-sm text-slate-500 py-1" data-test="closed">{{ t('dashboard.messages.closedNotice') }}</p>
            <template v-else>
              <NAlert v-if="sendError" tone="danger" class="mb-3">{{ sendError }}</NAlert>
              <MessageComposer id="thread-composer" v-model="draft" :conversation-id="selectedConversation.id" :busy="sending" @submit="send" />
            </template>
          </div>
        </template>
      </section>
    </div>

    <!-- Contact Nadoumi Admin Modal -->
    <NModal v-model="showNewModal" :title="t('dashboard.messages.contactAdmin')">
      <form class="space-y-4" @submit.prevent="submitNewConversation">
        <NAlert v-if="newError" tone="danger">{{ newError }}</NAlert>

        <NField :label="t('dashboard.messages.selectAdvisor')" for="new-advisor">
          <NSelect
            id="new-advisor"
            v-model="newAdminUserId"
            :options="adminSelectOptions"
          />
        </NField>

        <NField v-if="applicationSelectOptions.length > 1" :label="t('dashboard.messages.applicationId')" for="new-application">
          <NSelect
            id="new-application"
            v-model="newApplicationId"
            :options="applicationSelectOptions"
          />
        </NField>

        <NField :label="t('dashboard.messages.subject')" for="new-subject" required>
          <NInput
            id="new-subject"
            v-model="newSubject"
            :placeholder="t('dashboard.messages.subjectPlaceholder')"
            required
          />
        </NField>

        <NField :label="t('dashboard.messages.messageBody')" for="new-message-body" required>
          <NTextarea
            id="new-message-body"
            v-model="newMessageBody"
            :rows="4"
            :placeholder="t('dashboard.messages.messagePlaceholder')"
            required
          />
        </NField>

        <div class="flex justify-end gap-2 pt-2">
          <NButton type="button" variant="ghost" @click="showNewModal = false">
            {{ t('common.cancel') }}
          </NButton>
          <NButton type="submit" :loading="newSubmitting">
            {{ t('dashboard.messages.send') }}
          </NButton>
        </div>
      </form>
    </NModal>
  </div>
</template>
