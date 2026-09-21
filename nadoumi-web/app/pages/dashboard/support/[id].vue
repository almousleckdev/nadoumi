<script setup lang="ts">
import type { TicketDetail } from '~/types/support'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { get, reply } = useSupport()
const { user } = useSession()
const { busy, error: replyError, run } = useAsyncAction()

const POLL_MS = 30_000
const id = Number(route.params.id)

const detail = ref<TicketDetail | null>(null)
const pending = ref(true)
const error = ref('')
const thread = ref<{ reset: () => void } | null>(null)

const dtf = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium', timeStyle: 'short' }))
const formatTime = (iso: string) => dtf.value.format(new Date(iso))

async function load(silent = false) {
  if (!silent) pending.value = true
  try {
    detail.value = await get(id)
    error.value = ''
  }
  catch (e) {
    const status = (e as { statusCode?: number }).statusCode
    if (!silent) error.value = status === 404 ? t('dashboard.support.notFound') : t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

// Modest poll instead of a realtime stream: a support thread is slow-moving.
let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => { timer = setInterval(() => load(true), POLL_MS) })
onBeforeUnmount(() => { if (timer) clearInterval(timer) })

const closed = computed(() => (detail.value ? !canReplyToTicket(detail.value.ticket.status) : true))

async function onSend(body: string) {
  const sent = await run(() => reply(id, body))
  if (!sent) return
  thread.value?.reset()
  await load(true)
}

useSeo(detail.value?.ticket.subject ?? t('dashboard.support.title'), t('dashboard.support.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <NuxtLink :to="localePath('/dashboard/support')" class="text-sm font-medium text-brand-700 no-underline hover:underline">
      {{ t('dashboard.support.back') }}
    </NuxtLink>

    <AsyncState :pending="pending" :error="error">
      <template #loading>
        <div class="grid gap-3">
          <NSkeleton class="h-14 w-full rounded-lg" />
          <NSkeleton class="h-24 w-3/4 rounded-lg" />
          <NSkeleton class="h-24 w-3/4 rounded-lg" />
        </div>
      </template>
      <template #error>
        <NAlert tone="danger">
          {{ error }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>

      <template v-if="detail">
        <header class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <h1 class="font-display text-xl font-bold text-slate-900" data-test="subject">{{ detail.ticket.subject }}</h1>
            <p class="mt-1 text-sm text-slate-500">
              {{ t(`dashboard.support.category.${detail.ticket.category}`) }} · {{ formatTime(detail.ticket.createTime) }}
            </p>
          </div>
          <NBadge :tone="ticketStatusTone(detail.ticket.status)" data-test="status">
            {{ t(`dashboard.support.status.${detail.ticket.status}`) }}
          </NBadge>
        </header>

        <SectionCard :title="t('dashboard.support.conversation')">
          <SupportThread
            ref="thread"
            :messages="detail.messages"
            :current-user-id="user?.userId ?? null"
            :closed="closed"
            :busy="busy"
            :error="replyError"
            @send="onSend"
          />
        </SectionCard>
      </template>
    </AsyncState>
  </div>
</template>
