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


function computeSLA(t: any) {
  const end = new Date(t.resolvedAt || t.closedAt || t.updateTime).getTime()
  const start = new Date(t.createTime).getTime()
  const diff = end - start
  if (diff < 0) return '0h'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  return `${hours}h ${minutes}m`
}

const formatTime = (iso: string) =>
  formatSupportDate(iso, locale.value, { dateStyle: 'medium', timeStyle: 'short' })

async function load(silent = false) {
  if (!silent) pending.value = true
  try {
    detail.value = await get(id)
    error.value = ''
  }
  catch (e) {
    console.error('Failed to load support ticket detail:', e)
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


const { studentFetch } = useApi()
async function closeTicket() {
  const ok = await run(() => studentFetch(`/api/student/support/tickets/${id}/close`, { method: 'POST' }))
  if (ok) await load(true)
}

const showMeetingModal = ref(false)
const meetingDate = ref('')
const meetingTime = ref('')
const meetingDuration = ref<number>(35)
const meetingError = ref('')
const meetingBusy = ref(false)

async function bookMeeting() {
  if (!meetingDate.value || !meetingTime.value) {
    meetingError.value = 'Date and time are required'
    return
  }
  const startTime = `${meetingDate.value}T${meetingTime.value}:00`
  meetingBusy.value = true
  meetingError.value = ''
  try {
    await studentFetch(`/api/student/support/tickets/${id}/meetings`, {
      method: 'POST',
      body: { startTime, durationMinutes: meetingDuration.value }
    })
    showMeetingModal.value = false
    await load(true)
  } catch (e: any) {
    meetingError.value = e.data?.detail || 'Failed to book meeting'
  } finally {
    meetingBusy.value = false
  }
}

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
            <p v-if="detail.ticket.resolvedAt || detail.ticket.closedAt" class="mt-1 text-xs font-semibold text-slate-500">
              ⏱️ Time to Resolution: {{ computeSLA(detail.ticket) }}
            </p>

          </div>
          
          <NBadge :tone="ticketStatusTone(detail.ticket.status)" data-test="status">
            {{ t(`dashboard.support.status.${detail.ticket.status}`) }}
          </NBadge>
          <div class="ml-auto flex gap-2">
            <NButton v-if="detail.ticket.status !== 'CLOSED' && detail.ticket.status !== 'RESOLVED'" variant="primary" size="sm" @click="showMeetingModal = true">Request Meeting</NButton>
            <NButton v-if="detail.ticket.status !== 'CLOSED' && detail.ticket.status !== 'RESOLVED'" variant="secondary" size="sm" @click="closeTicket">{{ t('dashboard.support.action.CLOSED') || 'Close Ticket' }}</NButton>
          </div>
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

        <!-- Meeting Modal -->
        <NModal v-model:open="showMeetingModal" title="Request a 1:1 Meeting">
          <form @submit.prevent="bookMeeting" class="grid gap-4">
            <p class="text-sm text-slate-500">Select a date, time, and duration for your 1:1 meeting. If the requested slot conflicts with the assigned staff member's calendar, it will be rejected.</p>
            
            <NAlert v-if="meetingError" tone="danger">{{ meetingError }}</NAlert>
            
            <div class="grid gap-2">
              <label class="text-sm font-medium">Date</label>
              <NInput v-model="meetingDate" type="date" required />
            </div>
            
            <div class="grid gap-2">
              <label class="text-sm font-medium">Time</label>
              <NInput v-model="meetingTime" type="time" required />
            </div>
            
            <div class="grid gap-2">
              <label class="text-sm font-medium">Duration</label>
              <select v-model="meetingDuration" class="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-brand-600 sm:text-sm sm:leading-6">
                <option :value="35">35 minutes</option>
                <option :value="45">45 minutes</option>
                <option :value="60">1 hour</option>
                <option :value="90">1.5 hours</option>
              </select>
            </div>
            
            <div class="mt-4 flex justify-end gap-3">
              <NButton variant="secondary" @click="showMeetingModal = false">Cancel</NButton>
              <NButton variant="primary" type="submit" :loading="meetingBusy">Book Meeting</NButton>
            </div>
          </form>
        </NModal>
      </template>
    </AsyncState>
  </div>
</template>
