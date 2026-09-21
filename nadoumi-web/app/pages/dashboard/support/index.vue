<script setup lang="ts">
import type { TicketCategory, TicketStatus, TicketSummary } from '~/types/support'
import { TICKET_CATEGORIES, TICKET_STATUSES } from '~/types/support'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const localePath = useLocalePath()
const { list, create } = useSupport()
const { busy, error: createError, run } = useAsyncAction()

const PAGE_SIZE = 20
const SUBJECT_MAX = 200
const BODY_MAX = 4000

const items = ref<TicketSummary[]>([])
const page = ref(0)
const hasMore = ref(false)
const pending = ref(true)
const error = ref('')
const statusFilter = ref('')

const dtf = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }))
const formatDate = (iso: string) => dtf.value.format(new Date(iso))

const statusOptions = computed(() => [
  { value: '', label: t('dashboard.support.allStatuses') },
  ...TICKET_STATUSES.map(s => ({ value: s, label: t(`dashboard.support.status.${s}`) })),
])
const categoryOptions = computed(() =>
  TICKET_CATEGORIES.map(c => ({ value: c, label: t(`dashboard.support.category.${c}`) })))

async function load(reset = true) {
  if (reset) {
    pending.value = true
    page.value = 0
  }
  error.value = ''
  try {
    const rows = await list({
      status: (statusFilter.value || undefined) as TicketStatus | undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    items.value = reset ? rows : [...items.value, ...rows]
    hasMore.value = rows.length === PAGE_SIZE
  }
  catch {
    error.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

async function loadMore() {
  page.value += 1
  await load(false)
}

/* ---- New ticket ---- */
const showForm = ref(false)
const subject = ref('')
const category = ref<TicketCategory | ''>('')
const body = ref('')
const touched = ref(false)

const errors = computed(() => ({
  subject: subject.value.trim() ? '' : t('dashboard.support.errors.subject'),
  category: category.value ? '' : t('dashboard.support.errors.category'),
  body: body.value.trim() ? '' : t('dashboard.support.errors.body'),
}))
const formValid = computed(() => !errors.value.subject && !errors.value.category && !errors.value.body)

async function submit() {
  touched.value = true
  if (!formValid.value) return
  const created = await run(() => create({
    subject: subject.value.trim(),
    category: category.value as TicketCategory,
    body: body.value.trim(),
  }))
  if (created) await navigateTo(localePath(`/dashboard/support/${created.ticket.id}`))
}

useSeo(t('dashboard.support.title'), t('dashboard.support.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.support.title') }}</h1>
        <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.support.blurb') }}</p>
      </div>
      <NButton data-test="new-ticket" size="sm" @click="showForm = !showForm">{{ t('dashboard.support.newTicket') }}</NButton>
    </header>

    <SectionCard v-if="showForm" :title="t('dashboard.support.newTicket')">
      <form class="grid gap-4" novalidate @submit.prevent="submit">
        <NAlert v-if="createError" tone="danger">{{ createError }}</NAlert>
        <NField for="ticket-subject" :label="t('dashboard.support.subject')" :error="touched ? errors.subject : ''" required>
          <NInput id="ticket-subject" v-model="subject" :maxlength="SUBJECT_MAX" :invalid="touched && !!errors.subject" />
        </NField>
        <NField for="ticket-category" :label="t('dashboard.support.categoryLabel')" :error="touched ? errors.category : ''" required>
          <NSelect
            id="ticket-category"
            v-model="category"
            :options="categoryOptions"
            :placeholder="t('dashboard.support.categoryPlaceholder')"
            :invalid="touched && !!errors.category"
          />
        </NField>
        <NField for="ticket-body" :label="t('dashboard.support.message')" :error="touched ? errors.body : ''" required>
          <NTextarea id="ticket-body" v-model="body" :rows="5" :maxlength="BODY_MAX" :invalid="touched && !!errors.body" />
        </NField>
        <div class="flex gap-2">
          <NButton type="submit" :loading="busy">{{ t('dashboard.support.submit') }}</NButton>
          <NButton variant="ghost" :disabled="busy" @click="showForm = false">{{ t('common.cancel') }}</NButton>
        </div>
      </form>
    </SectionCard>

    <div class="max-w-xs">
      <label for="ticket-status-filter" class="sr-only">{{ t('dashboard.support.filterStatus') }}</label>
      <NSelect id="ticket-status-filter" v-model="statusFilter" :options="statusOptions" @update:model-value="load()" />
    </div>

    <AsyncState :pending="pending" :error="error" :empty="!items.length">
      <template #loading>
        <div class="grid gap-3">
          <NSkeleton class="h-16 w-full rounded-lg" />
          <NSkeleton class="h-16 w-full rounded-lg" />
          <NSkeleton class="h-16 w-full rounded-lg" />
        </div>
      </template>
      <template #error>
        <NAlert tone="danger">
          {{ error }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>
      <template #empty>
        <SectionCard :title="t('dashboard.support.title')">
          <p class="py-6 text-center text-sm text-slate-500">{{ t('dashboard.support.empty') }}</p>
        </SectionCard>
      </template>

      <SectionCard :title="t('dashboard.support.yourTickets')">
        <ul class="divide-y divide-slate-100">
          <li v-for="ticket in items" :key="ticket.id">
            <NuxtLink
              :to="localePath(`/dashboard/support/${ticket.id}`)"
              class="flex items-start justify-between gap-3 py-3 no-underline hover:bg-slate-50"
            >
              <span class="min-w-0">
                <span class="block truncate text-sm font-semibold text-slate-900">{{ ticket.subject }}</span>
                <span class="mt-0.5 block text-xs text-slate-500">
                  {{ t(`dashboard.support.category.${ticket.category}`) }} · {{ formatDate(ticket.updateTime) }}
                </span>
              </span>
              <NBadge :tone="ticketStatusTone(ticket.status)">{{ t(`dashboard.support.status.${ticket.status}`) }}</NBadge>
            </NuxtLink>
          </li>
        </ul>
      </SectionCard>

      <div v-if="hasMore" class="text-center">
        <NButton variant="secondary" size="sm" @click="loadMore">{{ t('common.loadMore') }}</NButton>
      </div>
    </AsyncState>
  </div>
</template>
