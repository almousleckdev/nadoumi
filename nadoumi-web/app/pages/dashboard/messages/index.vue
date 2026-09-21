<script setup lang="ts">
import type { ConversationSummary } from '~/types/messages'
import { formatMessageTime } from '~/utils/messages'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { listConversations } = useMessages()
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
    </header>

    <p v-if="reconnecting" class="text-xs text-amber-700" role="status">{{ t('dashboard.messages.reconnecting') }}</p>

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
