<script setup lang="ts">
import { computed } from 'vue'
import type { ChatConversation } from '~/types/chat'
import type { LoadStatus } from '~/composables/useChat'
import { filterInbox } from '~/utils/chat'

/** The conversation list: search, rows with presence/preview/unread, and loading, empty and error states. */
const props = defineProps<{
  items: ChatConversation[]
  status: LoadStatus
  activeId: number | null
  myId: number
  hasMore: boolean
  loadingMore: boolean
}>()
const emit = defineEmits<{ select: [id: number], new: [], retry: [], loadMore: [] }>()
const search = defineModel<string>('search', { required: true })
const { t } = useI18n()

const visible = computed(() => filterInbox(props.items, search.value))
const searchId = useId()
</script>

<template>
  <aside class="min-h-0 w-full flex-col border-slate-200 bg-white md:w-80 md:shrink-0 md:border-e lg:w-96" :aria-label="t('dashboard.messages.title')">
    <div class="flex items-center justify-between gap-2 px-4 pt-4">
      <h1 class="font-display text-lg font-bold text-slate-900">{{ t('dashboard.messages.title') }}</h1>
      <button
        type="button"
        class="inline-flex items-center gap-1.5 rounded-full bg-brand-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
        data-test="new-chat"
        @click="emit('new')"
      >
        <svg viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true"><path d="M12 5v14M5 12h14" stroke-linecap="round" /></svg>
        {{ t('dashboard.messages.newChat') }}
      </button>
    </div>

    <div class="px-4 py-3">
      <label :for="searchId" class="sr-only">{{ t('dashboard.messages.searchAria') }}</label>
      <div class="relative">
        <svg viewBox="0 0 24 24" class="pointer-events-none absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" stroke-linecap="round" />
        </svg>
        <input
          :id="searchId"
          v-model="search"
          type="search"
          autocomplete="off"
          :placeholder="t('dashboard.messages.searchPlaceholder')"
          class="w-full rounded-full border border-slate-200 bg-slate-50 py-2 ps-9 pe-3 text-sm text-slate-900 placeholder:text-slate-500 focus:border-brand-500 focus:bg-white focus:outline-none"
          data-test="inbox-search"
        >
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto">
      <ul v-if="status === 'loading'" class="grid gap-1 px-3" aria-busy="true" data-test="inbox-loading">
        <li v-for="n in 6" :key="n" class="flex items-center gap-3 py-2">
          <NSkeleton class="h-12 w-12 shrink-0 rounded-full" />
          <div class="grid flex-1 gap-2"><NSkeleton class="h-3.5 w-1/2 rounded" /><NSkeleton class="h-3 w-4/5 rounded" /></div>
        </li>
      </ul>

      <div v-else-if="status === 'error'" class="grid place-items-center gap-3 px-6 py-12 text-center" data-test="inbox-error">
        <p class="text-sm text-slate-700">{{ t('errors.loadSection') }}</p>
        <NButton size="sm" variant="secondary" @click="emit('retry')">{{ t('dashboard.messages.retry') }}</NButton>
      </div>

      <div v-else-if="items.length === 0" class="grid place-items-center gap-3 px-6 py-12 text-center" data-test="inbox-empty">
        <h2 class="font-display text-base font-semibold text-slate-900">{{ t('dashboard.messages.startConversation') }}</h2>
        <p class="text-sm text-slate-600">{{ t('dashboard.messages.startConversationDesc') }}</p>
        <NButton size="sm" @click="emit('new')">{{ t('dashboard.messages.newChat') }}</NButton>
      </div>

      <p v-else-if="visible.length === 0" class="px-6 py-12 text-center text-sm text-slate-600" data-test="inbox-no-results">
        {{ t('dashboard.messages.noResults') }}
      </p>

      <template v-else>
        <ul>
          <ChatInboxItem
            v-for="c in visible"
            :key="c.id"
            :conversation="c"
            :active="c.id === activeId"
            :my-id="myId"
            @select="emit('select', $event)"
          />
        </ul>
        <div v-if="hasMore && !search" class="p-3 text-center">
          <NButton size="sm" variant="ghost" :loading="loadingMore" @click="emit('loadMore')">{{ t('dashboard.messages.loadMore') }}</NButton>
        </div>
      </template>
    </div>
  </aside>
</template>
