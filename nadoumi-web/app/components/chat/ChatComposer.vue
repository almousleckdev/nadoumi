<script setup lang="ts">
import { ref, computed, watch, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { MESSAGE_ATTACHMENT_ACCEPT, MESSAGE_ATTACHMENT_MAX_MB, MESSAGE_ATTACHMENT_MIME } from '~/constants/messages'
import { MAX_ATTACHMENTS_PER_MESSAGE, MESSAGE_MAX_LENGTH } from '~/types/chat'
import { validateFile } from '~/utils/files'
import type { OutgoingFile } from '~/composables/useChat'

interface PendingFile { key: string, file: File, previewUrl: string | null, mediaId: number | null, uploading: boolean, error: boolean }

/**
 * The message box: auto-growing text, emoji, attach (button, drag and drop, paste), and send. Files upload as soon
 * as they are picked (upload, then attach) so a slow upload never blocks typing; Send waits for them to finish.
 * Enter sends, Shift+Enter adds a line.
 */
const props = defineProps<{ conversationId: number, disabled?: boolean }>()
const draft = defineModel<string>({ required: true })
const emit = defineEmits<{ send: [body: string, mediaIds: number[], files: OutgoingFile[]] }>()
const { t } = useI18n()
const { uploadAttachment } = useChatApi()

const MAX_HEIGHT_PX = 160
const COUNTER_FROM = MESSAGE_MAX_LENGTH - 200

const textarea = ref<HTMLTextAreaElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const pending = ref<PendingFile[]>([])
const problem = ref('')
const emojiOpen = ref(false)
const fileInputId = useId()

const uploading = computed(() => pending.value.some(p => p.uploading))
const readyIds = computed(() => pending.value.filter(p => p.mediaId !== null).map(p => p.mediaId!))
const canSend = computed(() =>
  !props.disabled && !uploading.value && (draft.value.trim().length > 0 || readyIds.value.length > 0))
const showCounter = computed(() => draft.value.length >= COUNTER_FROM)

function fit() {
  const el = textarea.value
  if (!el) return
  el.style.height = 'auto'
  const border = el.offsetHeight - el.clientHeight // scrollHeight excludes the border, so add it back or a scrollbar appears
  el.style.height = `${Math.min(el.scrollHeight + border, MAX_HEIGHT_PX)}px`
}
watch(draft, () => nextTick(fit))
onMounted(fit)

function releasePreviews() {
  for (const p of pending.value) if (p.previewUrl) URL.revokeObjectURL(p.previewUrl)
}
watch(() => props.conversationId, () => {
  releasePreviews()
  pending.value = []
  problem.value = ''
  emojiOpen.value = false
})
onBeforeUnmount(releasePreviews)

async function addFiles(files: File[]) {
  problem.value = ''
  for (const file of files) {
    if (pending.value.length >= MAX_ATTACHMENTS_PER_MESSAGE) {
      problem.value = t('dashboard.messages.tooManyFiles', { n: MAX_ATTACHMENTS_PER_MESSAGE })
      return
    }
    const issue = validateFile(file, { mime: MESSAGE_ATTACHMENT_MIME, maxMb: MESSAGE_ATTACHMENT_MAX_MB })
    if (issue) {
      problem.value = issue === 'badType'
        ? t('dashboard.messages.attachmentBadType')
        : t('dashboard.messages.attachmentTooBig', { name: file.name, mb: MESSAGE_ATTACHMENT_MAX_MB })
      continue
    }
    // reactive(): the upload result below is written through the proxy so canSend notices it
    const item = reactive<PendingFile>({
      key: `${file.name}-${file.size}-${Date.now()}-${pending.value.length}`,
      file, previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
      mediaId: null, uploading: true, error: false,
    })
    pending.value.push(item)
    void upload(item)
  }
}

async function upload(item: PendingFile) {
  item.uploading = true
  item.error = false
  try {
    item.mediaId = (await uploadAttachment(props.conversationId, item.file)).mediaId
  }
  catch {
    item.error = true
  }
  finally {
    item.uploading = false
  }
}

function remove(key: string) {
  const gone = pending.value.find(p => p.key === key)
  if (gone?.previewUrl) URL.revokeObjectURL(gone.previewUrl)
  pending.value = pending.value.filter(p => p.key !== key)
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = '' // picking the same file again must still fire
  void addFiles(files)
}

function onPaste(event: ClipboardEvent) {
  const files = Array.from(event.clipboardData?.files ?? [])
  if (files.length === 0) return
  event.preventDefault()
  void addFiles(files)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    submit()
  }
}

function insertEmoji(emoji: string) {
  const el = textarea.value
  if (!el) {
    draft.value += emoji
    return
  }
  const start = el.selectionStart ?? draft.value.length
  const end = el.selectionEnd ?? start
  draft.value = draft.value.slice(0, start) + emoji + draft.value.slice(end)
  emojiOpen.value = false
  nextTick(() => {
    el.focus()
    el.setSelectionRange(start + emoji.length, start + emoji.length)
  })
}

function submit() {
  if (!canSend.value) return
  const files = pending.value.filter(p => p.mediaId !== null).map(p => ({ name: p.file.name, type: p.file.type, size: p.file.size }))
  emit('send', draft.value, readyIds.value, files)
  releasePreviews()
  pending.value = []
  draft.value = ''
  problem.value = ''
  nextTick(() => textarea.value?.focus())
}

defineExpose({ addFiles, focus: () => textarea.value?.focus() })
</script>

<template>
  <form class="border-t border-slate-200 bg-white p-3 pe-[4.75rem] lg:pe-24" @submit.prevent="submit">
    <p v-if="problem" class="mb-2 rounded-md bg-red-50 px-3 py-1.5 text-sm text-red-700" role="alert">{{ problem }}</p>

    <ul v-if="pending.length" class="mb-2 flex flex-wrap gap-2" data-test="pending-files">
      <li
        v-for="p in pending"
        :key="p.key"
        class="flex max-w-full items-center gap-2 rounded-lg border bg-slate-50 py-1 ps-1 pe-1.5 text-xs"
        :class="p.error ? 'border-red-300 bg-red-50' : 'border-slate-200'"
        data-test="pending-file"
      >
        <img v-if="p.previewUrl" :src="p.previewUrl" alt="" class="h-9 w-9 rounded object-cover">
        <span v-else class="grid h-9 w-9 place-items-center rounded bg-brand-100 text-[10px] font-bold text-brand-800" aria-hidden="true">
          {{ (p.file.name.split('.').pop() ?? 'FILE').slice(0, 4).toUpperCase() }}
        </span>
        <span class="max-w-[10rem] truncate" :class="p.error ? 'text-red-700' : 'text-slate-700'">{{ p.file.name }}</span>
        <NSpinner v-if="p.uploading" class="h-3.5 w-3.5" />
        <button v-else-if="p.error" type="button" class="font-semibold text-brand-700 hover:underline" @click="upload(p)">
          {{ t('dashboard.messages.retry') }}
        </button>
        <button
          type="button"
          class="grid h-5 w-5 place-items-center rounded-full text-slate-500 hover:bg-slate-200"
          :aria-label="t('dashboard.messages.removeAttachment')"
          @click="remove(p.key)"
        >
          ×
        </button>
      </li>
    </ul>

    <div class="relative flex items-end gap-1.5">
      <label
        :for="fileInputId"
        class="grid h-10 w-10 shrink-0 cursor-pointer place-items-center rounded-full text-slate-600 hover:bg-slate-100 focus-within:outline focus-within:outline-2 focus-within:outline-brand-500"
        :class="disabled ? 'pointer-events-none opacity-50' : ''"
        :title="t('dashboard.messages.attach')"
      >
        <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path d="M21.44 11.05 12.25 20.24a5 5 0 0 1-7.07-7.07l8.49-8.49a3.5 3.5 0 0 1 4.95 4.95l-8.49 8.49a2 2 0 0 1-2.83-2.83l7.78-7.78" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="sr-only">{{ t('dashboard.messages.attach') }}</span>
        <input
          :id="fileInputId"
          ref="fileInput"
          type="file"
          multiple
          class="sr-only"
          :accept="MESSAGE_ATTACHMENT_ACCEPT"
          :disabled="disabled"
          @change="onPick"
        >
      </label>

      <div class="relative">
        <button
          type="button"
          class="grid h-10 w-10 shrink-0 place-items-center rounded-full text-slate-600 hover:bg-slate-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
          :class="disabled ? 'pointer-events-none opacity-50' : ''"
          :aria-label="t('dashboard.messages.emoji')"
          :aria-expanded="emojiOpen"
          data-test="emoji-toggle"
          @click="emojiOpen = !emojiOpen"
        >
          <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <circle cx="12" cy="12" r="9" /><path d="M8.5 14a4 4 0 0 0 7 0M9 9.5h.01M15 9.5h.01" stroke-linecap="round" />
          </svg>
        </button>
        <div v-if="emojiOpen" class="absolute bottom-full start-0 z-20 mb-2">
          <ChatEmojiPicker @pick="insertEmoji" @close="emojiOpen = false" />
        </div>
      </div>

      <textarea
        ref="textarea"
        v-model="draft"
        rows="1"
        dir="auto"
        :maxlength="MESSAGE_MAX_LENGTH"
        :disabled="disabled"
        :placeholder="disabled ? t('dashboard.messages.closedNotice') : t('dashboard.messages.composer')"
        :aria-label="t('dashboard.messages.composer')"
        class="max-h-40 min-h-[2.5rem] flex-1 resize-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2 text-[0.9375rem] leading-6 text-slate-900 placeholder:text-slate-500 focus:border-brand-500 focus:bg-white focus:outline-none disabled:opacity-60"
        data-test="composer"
        @keydown="onKeydown"
        @paste="onPaste"
      />

      <button
        type="submit"
        class="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-brand-600 text-white transition-colors hover:bg-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:pointer-events-none disabled:opacity-40"
        :disabled="!canSend"
        :aria-label="t('dashboard.messages.send')"
        data-test="send"
      >
        <svg viewBox="0 0 24 24" class="h-5 w-5 rtl:-scale-x-100" fill="currentColor" aria-hidden="true">
          <path d="M3.4 20.4 21 12 3.4 3.6l.1 6.5L15 12 3.5 13.9z" />
        </svg>
      </button>
    </div>
    <p v-if="showCounter" class="mt-1 text-end text-xs text-slate-500">{{ draft.length }} / {{ MESSAGE_MAX_LENGTH }}</p>
  </form>
</template>
