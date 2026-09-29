<script setup lang="ts">
import type { ConversationSummary } from '~/types/messages'
import { filterConversations, formatMessageTime } from '~/utils/messages'

const props = defineProps<{
  items: ConversationSummary[]
  pending: boolean
  error: string
  selectedId: number | null
  hasApplicant: boolean
}>()

const emit = defineEmits<{
  select: [id: number]
  retry: []
  create: []
}>()

const search = defineModel<string>('search', { required: true })

const { t, locale } = useI18n()

const filteredItems = computed(() => filterConversations(props.items, search.value))
const formatTime = (iso: string | null) => (iso ? formatMessageTime(iso, locale.value) : '')
</script>

<template>
<aside
  class="w-full md:w-80 lg:w-96 flex flex-col border-e border-slate-200 bg-slate-50/60"
  :class="{ 'hidden md:flex': selectedId !== null }"
>
  <!-- Search Bar -->
  <div class="p-3 border-b border-slate-200 bg-white">
    <NInput
      id="conv-search"
      v-model="search"
      :placeholder="t('dashboard.messages.searchPlaceholder')"
      :aria-label="t('dashboard.messages.searchAria')"
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
          <button type="button" class="ms-2 font-medium underline" @click="emit('retry')">
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
        <p v-if="hasApplicant" class="text-sm font-medium text-slate-700">{{ t('dashboard.messages.empty') }}</p>
        <p v-else class="text-sm font-medium text-slate-700">{{ t('dashboard.messages.noApplicant') }}</p>
        <NButton v-if="hasApplicant" class="mt-4" size="sm" @click="emit('create')">
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
            @click="emit('select', c.id)"
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
                    {{ t('dashboard.messages.staffBadge') }}
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
                <NBadge v-if="c.applicationId" size="xs" tone="neutral">{{ t('dashboard.messages.applicationRef', { id: c.applicationId }) }}</NBadge>
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
</template>
