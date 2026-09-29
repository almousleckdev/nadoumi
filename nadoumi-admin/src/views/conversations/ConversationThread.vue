<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { ConversationMessage, ConversationSummary } from '@/api/conversation'
import MessageBubble from './MessageBubble.vue'
import MessageComposer from './MessageComposer.vue'

const props = defineProps<{
  selected: ConversationSummary | null
  messages: ConversationMessage[]
  hasOlder: boolean
  loadingOlder: boolean
  loading: boolean
  error: string
  notParticipant: boolean
  canManage: boolean
  joining: boolean
  sending: boolean
  currentUserId: number | null
}>()

const emit = defineEmits<{
  close: []
  retry: []
  join: []
  loadOlder: []
  send: [payload: { body: string, attachmentMediaIds: number[] }]
}>()

const { t } = useI18n()

const scroller = ref<HTMLElement | null>(null)
const composer = ref<InstanceType<typeof MessageComposer>>()

const isClosed = computed(() => props.selected?.status === 'CLOSED')
const isParticipant = computed(() => !props.notParticipant && !props.loading && !props.error)

function isChained(index: number): boolean {
  return index > 0 && props.messages[index - 1]!.senderUserId === props.messages[index]!.senderUserId
}

function scrollToEnd() {
  nextTick(() => {
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
  })
}

function resetComposer() {
  composer.value?.reset()
}

defineExpose({ scrollToEnd, resetComposer })
</script>

<template>
  <section class="conv__thread">
    <EmptyState
      v-if="!selected"
      :title="t('conversations.pickTitle')"
      :description="t('conversations.pickDesc')"
      icon="ChatDotRound"
    />
    <template v-else>
      <header class="conv__thread-head">
        <div class="conv__thread-head-info">
          <h2 class="conv__thread-title">
            {{ selected.subject || t('conversations.untitled') }}
          </h2>
          <div class="conv__thread-badges">
            <el-tag
              v-if="selected.studentName"
              size="small"
              type="primary"
              effect="plain"
            >
              👤 {{ selected.studentName }}
            </el-tag>
            <el-tag
              v-if="selected.applicationId"
              size="small"
              type="success"
              effect="plain"
            >
              {{ t('conversations.applicationTag', { id: selected.applicationId }) }}
            </el-tag>
            <el-tag
              v-if="isClosed"
              size="small"
              type="info"
            >
              {{ t('conversations.closed') }}
            </el-tag>
          </div>
        </div>
        <el-button
          v-if="isParticipant && !isClosed"
          size="small"
          data-test="close"
          @click="emit('close')"
        >
          {{ t('conversations.close') }}
        </el-button>
      </header>

      <LoadingState
        v-if="loading"
        :rows="6"
      />
      <ErrorState
        v-else-if="error"
        :message="error"
        @retry="emit('retry')"
      />

      <div
        v-else-if="notParticipant"
        class="conv__join"
        data-test="join-panel"
      >
        <p>{{ t('conversations.notParticipant') }}</p>
        <el-button
          v-if="canManage"
          type="primary"
          :loading="joining"
          data-test="join"
          @click="emit('join')"
        >
          {{ t('conversations.join') }}
        </el-button>
        <p
          v-else
          class="conv__hint"
          data-test="join-denied"
        >
          {{ t('conversations.joinDenied') }}
        </p>
      </div>

      <template v-else>
        <div
          ref="scroller"
          class="conv__messages"
          data-test="thread"
        >
          <div
            v-if="hasOlder"
            class="conv__older"
          >
            <el-button
              size="small"
              text
              :loading="loadingOlder"
              data-test="load-older"
              @click="emit('loadOlder')"
            >
              {{ t('conversations.loadOlder') }}
            </el-button>
          </div>
          <p
            v-if="!messages.length"
            class="conv__hint"
          >
            {{ t('conversations.threadEmpty') }}
          </p>
          <MessageBubble
            v-for="(m, index) in messages"
            :key="m.id"
            :message="m"
            :mine="m.senderUserId === currentUserId"
            :chained="isChained(index)"
          />
        </div>

        <MessageComposer
          :key="selected.id"
          ref="composer"
          :conversation-id="selected.id"
          :closed="isClosed"
          :sending="sending"
          @send="payload => emit('send', payload)"
        />
      </template>
    </template>
  </section>
</template>

<style scoped>
.conv__thread { display: flex; flex-direction: column; }
.conv__thread-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 12px 16px; border-bottom: 1px solid var(--nad-line, #e5e7eb); }
.conv__thread-head-info { display: flex; flex-direction: column; gap: 4px; }
.conv__thread-title { margin: 0; display: inline; font-size: 16px; font-weight: 600; }
.conv__thread-badges { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.conv__messages { display: grid; gap: 10px; align-content: start; max-height: 460px; min-height: 240px; overflow-y: auto; padding: 16px; }
.conv__older { text-align: center; }
.conv__join { display: grid; gap: 12px; justify-items: center; padding: 48px 24px; text-align: center; }
.conv__hint { margin: 0; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
</style>
