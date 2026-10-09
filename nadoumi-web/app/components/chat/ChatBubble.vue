<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage, ReceiptStatus } from '~/types/chat'
import { clockTime } from '~/utils/chat'

/** One message. Mine sit on the end side in a brand tint, theirs on the start side in white. */
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
function relayPreview(url: string) {
  emit('preview', url)
}
/** Sent images form one grid; everything else (and images still uploading) stays a card. */
const isImage = (a: { image: boolean, id: number }) => a.image && a.id > 0
const images = computed(() => props.message.attachments.filter(isImage))
const files = computed(() => props.message.attachments.filter(a => !isImage(a)))
const time = computed(() => clockTime(props.message.createdAt, locale.value))
</script>

<template>
  <li class="flex" :class="mine ? 'justify-end' : 'justify-start'" :data-message-id="message.id">
    <div
      class="flex max-w-[85%] flex-col gap-1.5 rounded-2xl border px-3 py-2 shadow-sm sm:max-w-[70%]"
      :class="[
        mine ? 'border-brand-200 bg-brand-50 text-slate-900' : 'border-slate-200 bg-white text-slate-900',
        startsRun ? (mine ? 'rounded-te-md' : 'rounded-ts-md') : '',
        status === 'failed' ? 'border-red-300 bg-red-50' : '',
      ]"
    >
      <div v-if="images.length" class="grid gap-1" :class="images.length > 1 ? 'grid-cols-2' : ''" data-test="attachment-images">
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
      <p v-if="message.body" class="whitespace-pre-wrap break-words text-[0.9375rem] leading-relaxed" dir="auto">
        {{ message.body }}
      </p>
      <div class="flex items-center justify-end gap-1.5 text-[11px] text-slate-500">
        <template v-if="status === 'failed'">
          <span class="text-red-700">{{ t('dashboard.messages.notSent') }}</span>
          <button type="button" class="font-semibold text-brand-700 hover:underline" data-test="retry" @click="emit('retry', message.id)">
            {{ t('dashboard.messages.retry') }}
          </button>
          <button type="button" class="font-semibold text-slate-600 hover:underline" data-test="discard" @click="emit('discard', message.id)">
            {{ t('dashboard.messages.discard') }}
          </button>
        </template>
        <template v-else>
          <time :datetime="message.createdAt">{{ time }}</time>
          <ChatReceiptTicks v-if="mine" :status="status" />
        </template>
      </div>
    </div>
  </li>
</template>
