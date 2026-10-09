<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { ChatPerson, PresenceEvent } from '~/types/chat'
import { relativeTime } from '~/utils/chat'

/** Pick a staff member to start (or resume) a private chat with. Shows only a name, a photo and presence. */
const props = defineProps<{ modelValue: boolean, presence: Record<number, PresenceEvent> }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], pick: [staffUserId: number] }>()
const { t, locale } = useI18n()
const { listStaff } = useChatApi()

const staff = ref<ChatPerson[]>([])
const status = ref<'idle' | 'loading' | 'ready' | 'error'>('idle')
const query = ref('')
const searchId = useId()

async function load() {
  status.value = 'loading'
  try {
    staff.value = await listStaff()
    status.value = 'ready'
  }
  catch {
    status.value = 'error'
  }
}
watch(() => props.modelValue, (open) => {
  if (open) {
    query.value = ''
    void load()
  }
})

const people = computed(() => {
  const q = query.value.trim().toLowerCase()
  return staff.value
    .map(p => ({ ...p, ...(props.presence[p.userId] ? { online: props.presence[p.userId]!.online, lastSeenAt: props.presence[p.userId]!.lastSeenAt } : {}) }))
    .filter(p => !q || p.name.toLowerCase().includes(q))
    .sort((a, b) => Number(b.online) - Number(a.online) || a.name.localeCompare(b.name, locale.value))
})

function status0(p: ChatPerson): string {
  if (p.online) return t('dashboard.messages.online')
  return p.lastSeenAt ? t('dashboard.messages.lastSeen', { when: relativeTime(p.lastSeenAt, locale.value) }) : t('dashboard.messages.offline')
}
</script>

<template>
  <NModal :model-value="modelValue" :title="t('dashboard.messages.chooseAdvisor')" @update:model-value="emit('update:modelValue', $event)">
    <p class="mb-3 text-sm text-slate-600">{{ t('dashboard.messages.chooseAdvisorDesc') }}</p>
    <label :for="searchId" class="sr-only">{{ t('dashboard.messages.searchAdvisors') }}</label>
    <input
      :id="searchId"
      v-model="query"
      type="search"
      autocomplete="off"
      :placeholder="t('dashboard.messages.searchAdvisors')"
      class="mb-3 w-full rounded-full border border-slate-200 bg-slate-50 px-4 py-2 text-sm placeholder:text-slate-500 focus:border-brand-500 focus:bg-white focus:outline-none"
    >
    <div class="max-h-80 overflow-y-auto" data-test="staff-list">
      <ul v-if="status === 'loading'" class="grid gap-2" aria-busy="true">
        <li v-for="n in 4" :key="n" class="flex items-center gap-3"><NSkeleton class="h-10 w-10 rounded-full" /><NSkeleton class="h-4 w-1/2 rounded" /></li>
      </ul>
      <div v-else-if="status === 'error'" class="grid place-items-center gap-2 py-6 text-center">
        <p class="text-sm text-slate-700">{{ t('errors.loadSection') }}</p>
        <NButton size="sm" variant="secondary" @click="load">{{ t('dashboard.messages.retry') }}</NButton>
      </div>
      <p v-else-if="people.length === 0" class="py-6 text-center text-sm text-slate-600">{{ t('dashboard.messages.noAdvisors') }}</p>
      <ul v-else class="grid">
        <li v-for="p in people" :key="p.userId">
          <button
            type="button"
            class="flex w-full items-center gap-3 rounded-lg px-2 py-2 text-start hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline-none"
            data-test="staff-option"
            @click="emit('pick', p.userId)"
          >
            <ChatAvatar :name="p.name" :src="p.avatarUrl" size="md" :online="p.online" />
            <span class="min-w-0">
              <span class="block truncate text-sm font-semibold text-slate-900">{{ p.name }}</span>
              <span class="block truncate text-xs" :class="p.online ? 'text-emerald-700' : 'text-slate-500'">{{ status0(p) }}</span>
            </span>
          </button>
        </li>
      </ul>
    </div>
  </NModal>
</template>
