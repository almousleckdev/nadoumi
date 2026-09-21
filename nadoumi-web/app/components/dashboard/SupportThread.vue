<script setup lang="ts">
import type { TicketMessage } from '~/types/support'

const props = defineProps<{
  messages: TicketMessage[]
  currentUserId: number | null
  /** True when the ticket no longer accepts replies (RESOLVED or CLOSED). */
  closed: boolean
  busy: boolean
  error?: string
}>()
const emit = defineEmits<{ send: [body: string] }>()

const { t, locale } = useI18n()
const MAX_BODY = 4000
const draft = ref('')

const dtf = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium', timeStyle: 'short' }))
const formatTime = (iso: string) => dtf.value.format(new Date(iso))
const isOwn = (m: TicketMessage) => props.currentUserId !== null && m.senderUserId === props.currentUserId
const canSend = computed(() => !props.closed && !props.busy && draft.value.trim().length > 0)

function submit() {
  if (!canSend.value) return
  emit('send', draft.value.trim())
}

// The parent clears the draft after a successful send by calling this.
function reset() {
  draft.value = ''
}
defineExpose({ reset })
</script>

<template>
  <div class="grid gap-4">
    <ul class="grid gap-3" data-test="thread">
      <li
        v-for="m in messages"
        :key="m.id"
        class="max-w-[85%] rounded-lg px-4 py-3 text-sm"
        :class="isOwn(m) ? 'ms-auto bg-brand-50 text-slate-900' : 'me-auto border border-slate-200 bg-white text-slate-800'"
      >
        <p class="mb-1 text-xs font-medium text-slate-500">
          {{ isOwn(m) ? t('dashboard.support.you') : (m.senderName || t('dashboard.support.staff')) }}
          <span class="font-normal text-slate-400"> {{ formatTime(m.createdAt) }}</span>
        </p>
        <p class="whitespace-pre-wrap break-words">{{ m.body }}</p>
      </li>
    </ul>

    <NAlert v-if="closed" tone="info" data-test="closed-notice">{{ t('dashboard.support.closedNotice') }}</NAlert>
    <form v-else class="grid gap-2" @submit.prevent="submit">
      <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
      <label for="support-reply" class="sr-only">{{ t('dashboard.support.replyLabel') }}</label>
      <NTextarea
        id="support-reply"
        v-model="draft"
        :rows="3"
        :maxlength="MAX_BODY"
        :disabled="busy"
        :placeholder="t('dashboard.support.replyPlaceholder')"
      />
      <div>
        <NButton type="submit" size="sm" :loading="busy" :disabled="!canSend">{{ t('dashboard.support.send') }}</NButton>
      </div>
    </form>
  </div>
</template>
