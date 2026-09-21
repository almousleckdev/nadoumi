<script setup lang="ts">
import type { ConversationSummary } from '~/types/messages'
import { formatMessageTime } from '~/utils/messages'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const HTTP_FORBIDDEN = 403

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { listConversations, open } = useMessages()
const { primary } = useMyApplicant()

const items = ref<ConversationSummary[]>([])
const pending = ref(true)
const error = ref('')

async function load(silent = false) {
  if (!silent) pending.value = true
  error.value = ''
  try {
    items.value = await listConversations()
  }
  catch {
    if (!silent) error.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

const { reconnecting } = useConversationStream(() => load(true), () => load(true))

const composing = ref(false)
const subject = ref('')
const draft = ref('')
const { busy, error: sendError, run } = useAsyncAction()
const forbidden = ref(false)

async function startConversation() {
  const applicant = primary.value
  if (!applicant) return
  forbidden.value = false
  const sent = await run(async () => {
    try {
      return await open({ applicantId: applicant.id, subject: subject.value.trim() || undefined, body: draft.value.trim() })
    }
    catch (e) {
      const status = (e as { statusCode?: number, status?: number }).statusCode ?? (e as { status?: number }).status
      if (status === HTTP_FORBIDDEN) forbidden.value = true
      throw e
    }
  })
  if (!sent) return
  await navigateTo(localePath(`/dashboard/messages/${sent.conversationId}`))
}

function cancelCompose() {
  composing.value = false
  subject.value = ''
  draft.value = ''
  sendError.value = ''
  forbidden.value = false
}

const formatTime = (iso: string | null) => formatMessageTime(iso, locale.value)

useSeo(t('dashboard.messages.title'), t('dashboard.messages.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.messages.title') }}</h1>
        <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.messages.blurb') }}</p>
      </div>
      <NButton v-if="primary && !composing" size="sm" data-test="new-message" @click="composing = true">
        {{ t('dashboard.messages.newMessage') }}
      </NButton>
    </header>

    <p v-if="reconnecting" class="text-xs text-amber-700" role="status">{{ t('dashboard.messages.reconnecting') }}</p>

    <SectionCard v-if="composing" :title="t('dashboard.messages.newMessage')">
      <NAlert v-if="forbidden" tone="warning" class="mb-4" data-test="forbidden">{{ t('dashboard.messages.forbidden') }}</NAlert>
      <NAlert v-else-if="sendError" tone="danger" class="mb-4">{{ sendError }}</NAlert>
      <div class="grid gap-3">
        <NField :label="t('dashboard.messages.subject')" for="msg-subject">
          <NInput id="msg-subject" v-model="subject" :maxlength="200" />
        </NField>
        <NField :label="t('dashboard.messages.body')" for="msg-body" required>
          <MessageComposer id="msg-body" v-model="draft" :busy="busy" @submit="startConversation" />
        </NField>
        <div>
          <NButton variant="ghost" size="sm" :disabled="busy" @click="cancelCompose">{{ t('common.cancel') }}</NButton>
        </div>
      </div>
    </SectionCard>

    <AsyncState :pending="pending" :error="error" :empty="!items.length">
      <template #loading>
        <div class="grid gap-3">
          <NSkeleton v-for="n in 3" :key="n" class="h-16 w-full rounded-xl" />
        </div>
      </template>
      <template #error>
        <NAlert tone="danger">
          {{ error }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>
      <template #empty>
        <SectionCard :title="t('dashboard.messages.title')">
          <p v-if="primary" class="py-6 text-center text-sm text-slate-500">{{ t('dashboard.messages.empty') }}</p>
          <p v-else class="py-6 text-center text-sm text-slate-500">{{ t('dashboard.messages.noApplicant') }}</p>
        </SectionCard>
      </template>

      <ul class="grid gap-3">
        <li v-for="c in items" :key="c.id">
          <NuxtLink
            :to="localePath(`/dashboard/messages/${c.id}`)"
            class="flex items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 no-underline hover:border-brand-300"
            data-test="conversation-row"
          >
            <span class="min-w-0 flex-1">
              <span class="flex flex-wrap items-center gap-2">
                <span class="truncate text-sm" :class="c.unreadCount ? 'font-semibold text-slate-900' : 'font-medium text-slate-700'">
                  {{ c.subject || t('dashboard.messages.untitled') }}
                </span>
                <NBadge v-if="c.unreadCount" tone="brand">{{ t('dashboard.messages.unread', { count: c.unreadCount }) }}</NBadge>
                <NBadge v-if="c.status === 'CLOSED'">{{ t('dashboard.messages.closedBadge') }}</NBadge>
              </span>
              <span v-if="c.lastMessagePreview" class="mt-1 block truncate text-sm text-slate-500">{{ c.lastMessagePreview }}</span>
            </span>
            <span v-if="c.lastMessageAt" class="shrink-0 text-xs text-slate-400">{{ formatTime(c.lastMessageAt) }}</span>
          </NuxtLink>
        </li>
      </ul>
    </AsyncState>
  </div>
</template>
