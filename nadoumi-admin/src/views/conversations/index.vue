<template>
  <div class="nad-page">
    <PageHeader
      :title="t('conversations.title')"
      :subtitle="t('conversations.subtitle')"
    />

    <div
      class="chat"
      data-test="chat"
    >
      <ChatInbox
        v-model:search="studentSearch.query.value"
        class="chat__inbox"
        :class="{ 'chat__pane--hidden': chat.activeId.value !== null }"
        :items="chat.inbox.value"
        :status="chat.inboxStatus.value"
        :active-id="chat.activeId.value"
        :my-id="myId"
        :has-more="chat.hasMoreInbox.value"
        :loading-more="chat.loadingMoreInbox.value"
        :students="studentSearch.results.value"
        :students-status="studentSearch.status.value"
        @select="open"
        @start-with="startWith"
        @retry="chat.loadInbox({ query: studentSearch.query.value })"
        @load-more="chat.loadMoreInbox()"
      />
      <ChatThread
        v-model:draft="draft"
        class="chat__thread"
        :class="{ 'chat__pane--hidden': chat.activeId.value === null }"
        :conversation="chat.active.value"
        :messages="chat.messages.value"
        :status="chat.threadStatus.value"
        :has-older="chat.hasOlder.value"
        :loading-older="chat.loadingOlder.value"
        :older-failed="chat.olderFailed.value"
        :my-id="myId"
        :reconnecting="chat.reconnecting.value"
        @back="open(null)"
        @close="onClose"
        @load-older="chat.loadOlder()"
        @reload="chat.activeId.value && chat.select(chat.activeId.value)"
        @reopen="reopen"
        @send="chat.send"
        @retry="chat.retry"
        @discard="chat.discard"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useStaffChat } from '@/composables/useStaffChat'
import { useStudentSearch } from '@/composables/useStudentSearch'
import { useUserStore } from '@/stores/user'
import ChatInbox from './ChatInbox.vue'
import ChatThread from './ChatThread.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const myId = computed(() => userStore.userId ?? 0)
const chat = useStaffChat(() => myId.value)
// typing narrows the inbox on the server and finds students to start a chat with
const studentSearch = useStudentSearch(query => chat.loadInbox({ query }))

const drafts = reactive<Record<number, string>>({})
const draft = computed({
  get: () => (chat.activeId.value ? drafts[chat.activeId.value] ?? '' : ''),
  set: (value: string) => { if (chat.activeId.value) drafts[chat.activeId.value] = value },
})

async function open(id: number | null) {
  await chat.select(id)
  void router.replace({ query: { ...route.query, id: id ?? undefined } })
}

async function startWith(studentUserId: number) {
  try {
    const id = await chat.openWith(studentUserId)
    studentSearch.clear()
    void router.replace({ query: { ...route.query, id } })
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('conversations.loadError'))
  }
}

function reopen() {
  const peer = chat.active.value?.peer
  if (peer) void startWith(peer.userId)
}

async function onClose() {
  const id = chat.activeId.value
  if (id === null) return
  const confirmed = await confirm({
    title: t('conversations.close'), message: t('conversations.closeConfirm'), tone: 'danger',
  })
  if (!confirmed) return
  try {
    await chat.close(id)
    ElMessage.success(t('conversations.closedOk'))
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('conversations.loadError'))
  }
}

onMounted(async () => {
  await chat.loadInbox()
  const wanted = Number(route.query.id)
  if (wanted && chat.inbox.value.some(c => c.id === wanted)) await chat.select(wanted)
})
</script>

<style scoped>
.chat {
  display: flex;
  height: calc(100vh - var(--nad-header-h) - 170px);
  min-height: 480px;
  overflow: hidden;
  border: 1px solid var(--nad-line);
  border-radius: var(--nad-radius);
  background: var(--nad-surface);
  box-shadow: var(--nad-shadow-sm);
}
@media (max-width: 900px) {
  .chat__pane--hidden { display: none; }
}
</style>
