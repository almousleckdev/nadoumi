<script setup lang="ts">
import type { NotificationView } from '~/types/catalog'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t, locale } = useI18n()
const { list, markRead, markAllRead } = useNotifications()

const dtf = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium', timeStyle: 'short' }))
const formatCreatedAt = (iso: string) => dtf.value.format(new Date(iso))

const PAGE_SIZE = 20
const items = ref<NotificationView[]>([])
const page = ref(0)
const total = ref(0)
const pending = ref(true)
const error = ref('')

async function load(reset = true) {
  if (reset) { pending.value = true; page.value = 0 }
  error.value = ''
  try {
    const res = await list({ page: page.value, size: PAGE_SIZE })
    items.value = reset ? res.content : [...items.value, ...res.content]
    total.value = res.totalElements
  }
  catch {
    error.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

const hasMore = computed(() => items.value.length < total.value)
async function loadMore() {
  page.value += 1
  await load(false)
}


const router = useRouter()
const localePath = useLocalePath()

async function onOpen(n: NotificationView) {
  if (!n.read) {
    n.read = true
    n.readAt = new Date().toISOString()
    await markRead(n.id).catch(() => { n.read = false; n.readAt = null })
  }
  
  // Deep-linking based on notification type
  const t = n.type || ''
  if (t === 'CONTACT_INQUIRY_RECEIVED' || t.includes('MESSAGE')) {
    router.push(localePath('/dashboard/messages'))
  } else if (t.includes('TICKET') || t.includes('SUPPORT')) {
    router.push(localePath('/dashboard/support'))
  } else if (t.includes('APPLICATION') || t === 'TASK_PROGRESS') {
    router.push(localePath('/dashboard/applications'))
  }
}
)
}

const unreadExists = computed(() => items.value.some((n: NotificationView) => !n.read))
async function onMarkAll() {
  const previouslyUnread = items.value.filter((n: NotificationView) => !n.read)
  items.value.forEach((n: NotificationView) => { n.read = true; n.readAt = n.readAt ?? new Date().toISOString() })
  await markAllRead().catch(() => { previouslyUnread.forEach((n: NotificationView) => { n.read = false }) })
}

useSeo(t('dashboard.notifications.title'), t('dashboard.notifications.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="font-display text-xl font-bold text-slate-900">{{ t('dashboard.notifications.title') }}</h1>
        <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.notifications.blurb') }}</p>
      </div>
      <NButton v-if="unreadExists" variant="secondary" size="sm" @click="onMarkAll">{{ t('dashboard.notifications.markAllRead') }}</NButton>
    </header>

    <AsyncState :pending="pending" :error="error" :empty="!items.length">
      <template #error>
        <NAlert tone="danger">
          {{ error }}
          <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
        </NAlert>
      </template>
      <template #empty>
        <SectionCard :title="t('dashboard.notifications.title')">
          <p class="py-6 text-center text-sm text-slate-500">{{ t('dashboard.notifications.empty') }}</p>
        </SectionCard>
      </template>

      <SectionCard :title="t('dashboard.notifications.title')">
        <ul class="divide-y divide-slate-100">
          <li v-for="n in items" :key="n.id">
            <button
              type="button"
              class="flex w-full items-start gap-3 py-3 text-start"
              @click="onOpen(n)"
            >
              <span
                class="mt-1.5 h-2 w-2 shrink-0 rounded-full"
                :class="n.read ? 'bg-transparent' : 'bg-brand-600'"
                aria-hidden="true"
              />
              <span v-if="n.type && (n.type.includes('PUBLISHED') || n.type === 'PLATFORM_UPDATE')" class="mt-0.5 shrink-0 text-slate-400" title="System Announcement">
                📢
              </span>
              <span class="min-w-0 flex-1">
                <span class="block text-sm" :class="n.read ? 'font-medium text-slate-700' : 'font-semibold text-slate-900'">
                  <span v-if="n.type && n.type.includes('PUBLISHED')" class="inline-flex items-center rounded-md bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10 mr-2">System</span>
                  {{ n.title }}
                </span>
                <span class="mt-0.5 block text-sm text-slate-500">{{ n.body }}</span>
                <span class="mt-1 block text-xs text-slate-400">{{ formatCreatedAt(n.createdAt) }}</span>
              </span>
            </button>
          </li>
        </ul>
      </SectionCard>

      <div v-if="hasMore" class="text-center">
        <NButton variant="secondary" size="sm" @click="loadMore">{{ t('common.loadMore') }}</NButton>
      </div>
    </AsyncState>
  </div>
</template>
