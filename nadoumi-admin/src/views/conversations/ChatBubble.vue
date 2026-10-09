<template>
  <li
    class="cb"
    :class="mine ? 'cb--mine' : 'cb--theirs'"
    :data-message-id="message.id"
  >
    <div
      class="cb__bubble"
      :class="{ 'cb__bubble--start': startsRun, 'cb__bubble--failed': status === 'failed' }"
    >
      <div
        v-if="images.length"
        class="cb__images"
        :class="{ 'cb__images--grid': images.length > 1 }"
        data-test="attachment-images"
      >
        <ChatAttachment
          v-for="a in images"
          :key="a.id"
          :conversation-id="conversationId"
          :attachment="a"
          :mine="mine"
          :compact="images.length > 1"
          @preview="relayPreview"
        />
      </div>
      <ChatAttachment
        v-for="a in files"
        :key="a.id"
        :conversation-id="conversationId"
        :attachment="a"
        :mine="mine"
        @preview="relayPreview"
      />
      <p
        v-if="message.body"
        class="cb__text"
        dir="auto"
      >
        {{ message.body }}
      </p>
      <div class="cb__foot">
        <template v-if="status === 'failed'">
          <span class="cb__err">{{ t('conversations.notSent') }}</span>
          <button
            type="button"
            class="cb__link"
            data-test="retry"
            @click="emit('retry', message.id)"
          >
            {{ t('conversations.retry') }}
          </button>
          <button
            type="button"
            class="cb__link cb__link--quiet"
            data-test="discard"
            @click="emit('discard', message.id)"
          >
            {{ t('conversations.discard') }}
          </button>
        </template>
        <template v-else>
          <time :datetime="message.createdAt">{{ time }}</time>
          <ReceiptTicks
            v-if="mine"
            :status="status"
          />
        </template>
      </div>
    </div>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChatMessage, ReceiptStatus } from '@/api/conversation'
import { clockTime } from '@/utils/chat'
import ChatAttachment from './ChatAttachment.vue'
import ReceiptTicks from './ReceiptTicks.vue'

/** One message. Mine sit on the end side in a brand tint, the student's on the start side in white. */
const props = defineProps<{
  message: ChatMessage
  mine: boolean
  status: ReceiptStatus
  conversationId: number
  /** First of a run from the same sender: gets the squared corner that points at the speaker. */
  startsRun: boolean
}>()
const emit = defineEmits<{ preview: [url: string], retry: [id: number], discard: [id: number] }>()
const { t, locale } = useI18n()
/** Sent images form one grid; everything else (and images still uploading) stays a card. */
const isImage = (a: { image: boolean, id: number }) => a.image && a.id > 0
const images = computed(() => props.message.attachments.filter(isImage))
const files = computed(() => props.message.attachments.filter(a => !isImage(a)))
const time = computed(() => clockTime(props.message.createdAt, locale.value))
function relayPreview(url: string) {
  emit('preview', url)
}
</script>

<style scoped>
.cb { display: flex; list-style: none; }
.cb--mine { justify-content: flex-end; }
.cb__bubble {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: min(70%, 520px);
  padding: 8px 12px;
  border: 1px solid var(--nad-line);
  border-radius: 16px;
  background: #fff;
  box-shadow: var(--nad-shadow-sm);
  color: var(--nad-ink);
}
.cb--mine .cb__bubble { border-color: var(--nad-brand-200); background: var(--nad-brand-50); }
.cb__bubble--start { border-start-start-radius: 4px; }
.cb--mine .cb__bubble--start { border-start-start-radius: 16px; border-start-end-radius: 4px; }
.cb__bubble--failed { border-color: #fca5a5; background: #fef2f2; }
.cb__images { display: grid; gap: 4px; }
.cb__images--grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.cb__text { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 15px; line-height: 1.5; }
.cb__foot { display: flex; align-items: center; justify-content: flex-end; gap: 6px; font-size: 11px; color: var(--nad-ink-soft); }
.cb__err { color: #b91c1c; }
.cb__link { padding: 0; border: 0; background: none; color: var(--nad-brand-700); font-size: 11px; font-weight: 600; cursor: pointer; }
.cb__link--quiet { color: var(--nad-ink-soft); }
.cb__link:hover { text-decoration: underline; }
</style>
