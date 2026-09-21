<script setup lang="ts">
import { MESSAGE_PAGE_SIZE, type ConversationMessage, type ConversationSummary } from '~/types/messages'
import { formatMessageTime, mergeMessages } from '~/utils/messages'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { user } = useSession()
const { listConversations, listMessages, post, markRead } = useMessages()

const conversationId = Number(route.params.id)

const conversation = ref<ConversationSummary | null>(null)
const messages = ref<ConversationMessage[]>([])
const hasOlder = ref(false)
const pending = ref(true)
const error = ref('')
const loadingOlder = ref(false)
const olderError = ref('')
const scroller = ref<HTMLElement | null>(null)

function scrollToEnd() {
  nextTick(() => {
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
  })
}

function acknowledge() {
  // Best effort: an unread badge that lingers is harmless, a failed thread load is not.
  markRead(conversationId).catch(() => undefined)
}

async function load() {
  pending.value = true
  error.value = ''
  try {
    const [list, page] = await Promise.all([listConversations(), listMessages(conversationId)])
    conversation.value = list.find(c => c.id === conversationId) ?? null
    if (!conversation.value) {
      error.value = t('errors.loadSection')
      return
    }
    messages.value = mergeMessages([], page)
    hasOlder.value = page.length === MESSAGE_PAGE_SIZE
    acknowledge()
    scrollToEnd()
  }
  catch {
    error.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

async function syncLatest() {
  try {
    const before = messages.value.length
    messages.value = mergeMessages(messages.value, await listMessages(conversationId))
    if (messages.value.length !== before) {
      acknowledge()
      scrollToEnd()
    }
  }
  catch {
    // the next ping or resync retries; a transient failure must not replace the thread with an error
  }
}

const { reconnecting } = useConversationStream(
  (refId) => { if (refId === conversationId) void syncLatest() },
  () => void syncLatest(),
)

async function loadOlder() {
  const oldest = messages.value[0]
  if (!oldest || loadingOlder.value) return
  loadingOlder.value = true
  olderError.value = ''
  try {
    const page = await listMessages(conversationId, oldest.id)
    messages.value = mergeMessages(messages.value, page)
    hasOlder.value = page.length === MESSAGE_PAGE_SIZE
  }
  catch {
    olderError.value = t('errors.loadSection')
  }
  finally {
    loadingOlder.value = false
  }
}

const draft = ref('')
const { busy, error: sendError, run } = useAsyncAction()

async function send() {
  const body = draft.value.trim()
  const sent = await run(() => post(conversationId, body))
  if (!sent) return
  draft.value = ''
  messages.value = mergeMessages(messages.value, [sent])
  scrollToEnd()
}

const isClosed = computed(() => conversation.value?.status === 'CLOSED')
const isMine = (m: ConversationMessage) => m.senderUserId === user.value?.userId
const formatTime = (iso: string) => formatMessageTime(iso, locale.value)
const title = computed(() => conversation.value?.subject || t('dashboard.messages.untitled'))

useSeo(t('dashboard.messages.title'), t('dashboard.messages.blurb'))
</script>

<template>
  <div class="grid gap-4">
    <div>
      <NuxtLink :to="localePath('/dashboard/messages')" class="text-sm font-medium text-brand-700 hover:underline" data-test="back">
        {{ t('dashboard.messages.back') }}
      </NuxtLink>
    </div>

    <AsyncState :pending="pending" :error="error">
      <template #loading>
        <div class="grid gap-3">
          <NSkeleton class="h-8 w-1/3 rounded-md" />
          <NSkeleton class="h-16 w-2/3 rounded-xl" />
          <NSkeleton class="ms-auto h-16 w-2/3 rounded-xl" />
          <NSkeleton class="h-16 w-1/2 rounded-xl" />
        </div>
      </template>
      <template #error>
        <NAlert tone="danger">
          {{ error }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>

      <SectionCard :title="title">
        <template #actions>
          <NBadge v-if="isClosed">{{ t('dashboard.messages.closedBadge') }}</NBadge>
        </template>

        <p v-if="reconnecting" class="mb-3 text-xs text-amber-700" role="status">{{ t('dashboard.messages.reconnecting') }}</p>

        <div ref="scroller" class="grid max-h-[28rem] gap-3 overflow-y-auto pe-1" data-test="thread">
          <div v-if="hasOlder" class="text-center">
            <NButton variant="ghost" size="sm" :loading="loadingOlder" data-test="load-older" @click="loadOlder">
              {{ t('dashboard.messages.loadOlder') }}
            </NButton>
            <p v-if="olderError" class="mt-1 text-xs text-red-600" role="alert">{{ olderError }}</p>
          </div>
          <p v-if="!messages.length" class="py-6 text-center text-sm text-slate-500">{{ t('dashboard.messages.threadEmpty') }}</p>

          <div
            v-for="m in messages"
            :key="m.id"
            class="flex"
            :class="isMine(m) ? 'justify-end' : 'justify-start'"
            data-test="message"
          >
            <div
              class="max-w-[85%] rounded-2xl px-4 py-2 text-sm"
              :class="isMine(m) ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-800'"
            >
              <p class="text-xs font-semibold opacity-80">{{ isMine(m) ? t('dashboard.messages.you') : m.senderName }}</p>
              <p class="mt-0.5 whitespace-pre-wrap break-words">{{ m.body }}</p>
              <ul v-if="m.attachments.length" class="mt-1 grid gap-0.5 text-xs opacity-90">
                <li v-for="a in m.attachments" :key="a.id">{{ t('dashboard.messages.attachment') }}: {{ a.filename }}</li>
              </ul>
              <p class="mt-1 text-end text-[11px] opacity-70">{{ formatTime(m.createdAt) }}</p>
            </div>
          </div>
        </div>

        <div class="mt-4 border-t border-slate-100 pt-4">
          <p v-if="isClosed" class="text-sm text-slate-500" data-test="closed">{{ t('dashboard.messages.closedNotice') }}</p>
          <template v-else>
            <NAlert v-if="sendError" tone="danger" class="mb-3">{{ sendError }}</NAlert>
            <MessageComposer id="thread-composer" v-model="draft" :busy="busy" @submit="send" />
          </template>
        </div>
      </SectionCard>
    </AsyncState>
  </div>
</template>
