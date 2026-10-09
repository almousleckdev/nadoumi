<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const chat = useChat()

const search = ref('')
const picking = ref(false)
const drafts = reactive<Record<number, string>>({})
const draft = computed({
  get: () => (chat.activeId.value ? drafts[chat.activeId.value] ?? '' : ''),
  set: (value: string) => { if (chat.activeId.value) drafts[chat.activeId.value] = value },
})

async function open(id: number | null) {
  await chat.select(id)
  void router.replace({ query: { ...route.query, id: id ?? undefined } })
}

async function startWith(staffUserId: number) {
  picking.value = false
  const id = await chat.openWith(staffUserId)
  void router.replace({ query: { ...route.query, id } })
}

function reopen() {
  const peer = chat.active.value?.peer
  if (peer) void startWith(peer.userId)
}

// a chat that disappears (deleted by staff) must not leave its id in the address bar
watch(() => chat.activeId.value, (id) => {
  if (id === null && route.query.id) void router.replace({ query: { ...route.query, id: undefined } })
})

onMounted(async () => {
  await chat.loadInbox()
  const wanted = Number(route.query.id)
  if (wanted && chat.inbox.value.some(c => c.id === wanted)) await chat.select(wanted)
})

useSeo(t('dashboard.messages.title'), t('dashboard.messages.blurb'))
</script>

<template>
  <div class="flex h-[calc(100dvh-9rem)] min-h-[520px] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm" data-test="chat">
    <ChatInbox
      v-model:search="search"
      class="md:flex"
      :class="chat.activeId.value ? 'hidden' : 'flex'"
      :items="chat.inbox.value"
      :status="chat.inboxStatus.value"
      :active-id="chat.activeId.value"
      :my-id="chat.myId.value"
      :has-more="chat.hasMoreInbox.value"
      :loading-more="chat.loadingMoreInbox.value"
      @select="open"
      @new="picking = true"
      @retry="chat.loadInbox()"
      @load-more="chat.loadMoreInbox()"
    />
    <ChatThread
      v-model:draft="draft"
      class="md:flex"
      :class="chat.activeId.value ? 'flex' : 'hidden'"
      :conversation="chat.active.value"
      :messages="chat.messages.value"
      :status="chat.threadStatus.value"
      :has-older="chat.hasOlder.value"
      :loading-older="chat.loadingOlder.value"
      :older-failed="chat.olderFailed.value"
      :my-id="chat.myId.value"
      :reconnecting="chat.reconnecting.value"
      @back="open(null)"
      @load-older="chat.loadOlder()"
      @reload="chat.activeId.value && chat.select(chat.activeId.value)"
      @reopen="reopen"
      @send="chat.send"
      @retry="chat.retry"
      @discard="chat.discard"
    />
    <ChatStaffPicker v-model="picking" :presence="chat.presenceByUser" @pick="startWith" />
  </div>
</template>
