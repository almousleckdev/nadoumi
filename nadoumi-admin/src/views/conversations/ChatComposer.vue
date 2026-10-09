<template>
  <form
    class="cc"
    @submit.prevent="submit"
  >
    <p
      v-if="problem"
      class="cc__problem"
      role="alert"
    >
      {{ problem }}
    </p>

    <ul
      v-if="pending.length"
      class="cc__files"
      data-test="pending-files"
    >
      <li
        v-for="p in pending"
        :key="p.key"
        class="cc__file"
        :class="{ 'cc__file--error': p.error }"
        data-test="pending-file"
      >
        <img
          v-if="p.previewUrl"
          :src="p.previewUrl"
          alt=""
          class="cc__thumb"
        >
        <span
          v-else
          class="cc__kind"
          aria-hidden="true"
        >{{ (p.file.name.split('.').pop() ?? 'FILE').slice(0, 4).toUpperCase() }}</span>
        <span class="cc__fname">{{ p.file.name }}</span>
        <el-icon
          v-if="p.uploading"
          class="is-loading"
          :size="14"
        >
          <Loading />
        </el-icon>
        <button
          v-else-if="p.error"
          type="button"
          class="cc__retry"
          @click="upload(p)"
        >
          {{ t('conversations.retry') }}
        </button>
        <button
          type="button"
          class="cc__x"
          :aria-label="t('conversations.removeAttachment')"
          @click="remove(p.key)"
        >
          ×
        </button>
      </li>
    </ul>

    <div class="cc__row">
      <label
        :for="fileInputId"
        class="cc__icon"
        :class="{ 'cc__icon--off': disabled }"
        :title="t('conversations.attach')"
      >
        <el-icon :size="20"><Paperclip /></el-icon>
        <span class="sr-only">{{ t('conversations.attach') }}</span>
        <input
          :id="fileInputId"
          type="file"
          multiple
          class="sr-only"
          :accept="MESSAGE_ATTACHMENT_ACCEPT"
          :disabled="disabled"
          @change="onPick"
        >
      </label>

      <div class="cc__emoji">
        <button
          type="button"
          class="cc__icon"
          :class="{ 'cc__icon--off': disabled }"
          :aria-label="t('conversations.emoji')"
          :aria-expanded="emojiOpen"
          data-test="emoji-toggle"
          @click="emojiOpen = !emojiOpen"
        >
          <el-icon :size="20">
            <Sunny />
          </el-icon>
        </button>
        <div
          v-if="emojiOpen"
          class="cc__pop"
        >
          <EmojiPicker
            @pick="insertEmoji"
            @close="emojiOpen = false"
          />
        </div>
      </div>

      <textarea
        ref="textarea"
        v-model="draft"
        rows="1"
        dir="auto"
        class="cc__input"
        :maxlength="MESSAGE_MAX_LENGTH"
        :disabled="disabled"
        :placeholder="disabled ? t('conversations.closedNotice') : t('conversations.composer')"
        :aria-label="t('conversations.composer')"
        data-test="composer"
        @keydown="onKeydown"
        @paste="onPaste"
      />

      <button
        type="submit"
        class="cc__send"
        :disabled="!canSend"
        :aria-label="t('conversations.send')"
        data-test="send"
      >
        <el-icon :size="18">
          <Promotion />
        </el-icon>
      </button>
    </div>
    <p
      v-if="showCounter"
      class="cc__count"
    >
      {{ draft.length }} / {{ MESSAGE_MAX_LENGTH }}
    </p>
  </form>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Loading, Paperclip, Promotion, Sunny } from '@element-plus/icons-vue'
import {
  MAX_ATTACHMENTS_PER_MESSAGE, MESSAGE_ATTACHMENT_ACCEPT, MESSAGE_ATTACHMENT_MAX_MB, MESSAGE_ATTACHMENT_MIME,
  MESSAGE_MAX_LENGTH, uploadAttachment,
} from '@/api/conversation'
import type { OutgoingFile } from '@/composables/useStaffChat'
import EmojiPicker from './EmojiPicker.vue'

interface PendingFile { key: string, file: File, previewUrl: string | null, mediaId: number | null, uploading: boolean, error: boolean }

const BYTES_PER_MB = 1024 * 1024
const MAX_HEIGHT_PX = 160
const COUNTER_FROM = MESSAGE_MAX_LENGTH - 200

/**
 * The reply box: auto-growing text, emoji, attach (button, drag and drop, paste), and send. Files upload as soon as
 * they are picked (upload, then attach), so a slow upload never blocks typing; Send waits for them to finish.
 * Enter sends, Shift+Enter adds a line.
 */
const props = defineProps<{ conversationId: number, disabled?: boolean }>()
const draft = defineModel<string>({ required: true })
const emit = defineEmits<{ send: [body: string, mediaIds: number[], files: OutgoingFile[]] }>()
const { t } = useI18n()

const textarea = ref<HTMLTextAreaElement | null>(null)
const pending = ref<PendingFile[]>([])
const problem = ref('')
const emojiOpen = ref(false)
const fileInputId = `chat-file-${Math.random().toString(36).slice(2)}`

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
      problem.value = t('conversations.tooManyFiles', { n: MAX_ATTACHMENTS_PER_MESSAGE })
      return
    }
    if (!MESSAGE_ATTACHMENT_MIME.test(file.type)) {
      problem.value = t('conversations.attachmentBadType')
      continue
    }
    if (file.size > MESSAGE_ATTACHMENT_MAX_MB * BYTES_PER_MB) {
      problem.value = t('conversations.attachmentTooBig', { name: file.name, mb: MESSAGE_ATTACHMENT_MAX_MB })
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
  // the image previews now belong to the message being sent, which releases them once the server has answered
  const files = pending.value.filter(p => p.mediaId !== null)
    .map(p => ({ name: p.file.name, type: p.file.type, size: p.file.size, previewUrl: p.previewUrl ?? undefined }))
  emit('send', draft.value, readyIds.value, files)
  pending.value = []
  draft.value = ''
  problem.value = ''
  nextTick(() => textarea.value?.focus())
}

defineExpose({ addFiles, focus: () => textarea.value?.focus() })
</script>

<style scoped>
.cc { padding: 12px; border-top: 1px solid var(--nad-line); background: #fff; }
.cc__problem { margin: 0 0 8px; padding: 6px 12px; border-radius: 6px; background: #fef2f2; color: #b91c1c; font-size: 13px; }
.cc__files { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 8px; padding: 0; list-style: none; }
.cc__file { display: flex; align-items: center; gap: 8px; max-width: 100%; padding: 4px 6px 4px 4px; border: 1px solid var(--nad-line); border-radius: 8px; background: #f8fafc; font-size: 12px; }
.cc__file--error { border-color: #fca5a5; background: #fef2f2; }
.cc__thumb { width: 36px; height: 36px; border-radius: 4px; object-fit: cover; }
.cc__kind { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 4px; background: var(--nad-brand-100); color: var(--nad-brand-700); font-size: 10px; font-weight: 700; }
.cc__fname { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cc__retry { padding: 0; border: 0; background: none; color: var(--nad-brand-700); font-size: 12px; font-weight: 600; cursor: pointer; }
.cc__x { display: grid; place-items: center; width: 20px; height: 20px; border: 0; border-radius: 50%; background: transparent; color: var(--nad-ink-soft); cursor: pointer; }
.cc__x:hover { background: #e2e8f0; }
.cc__row { display: flex; align-items: flex-end; gap: 4px; padding: 6px; border: 1px solid var(--nad-line); border-radius: 24px; background: #f8fafc; transition: border-color 0.15s, background 0.15s; }
.cc__row:focus-within { border-color: var(--nad-brand-500); background: #fff; }
.cc__icon { display: grid; place-items: center; flex-shrink: 0; width: 40px; height: 40px; border: 0; border-radius: 50%; background: transparent; color: var(--nad-ink-soft); cursor: pointer; }
.cc__icon:hover { background: #f1f5f9; }
.cc__icon:focus-within, .cc__icon:focus-visible { outline: 2px solid var(--nad-brand-500); }
.cc__icon--off { pointer-events: none; opacity: 0.5; }
.cc__emoji { position: relative; }
.cc__pop { position: absolute; inset-block-end: 100%; inset-inline-start: 0; z-index: 20; margin-bottom: 8px; }
.cc__input { box-sizing: border-box; flex: 1; min-height: 40px; max-height: 160px; padding: 9px 8px; border: 0; background: transparent; color: var(--nad-ink); font: inherit; font-size: 15px; line-height: 22px; resize: none; }
.cc__input::placeholder { color: var(--nad-ink-soft); }
.cc__input:focus { outline: none; }
.cc__input:disabled { opacity: 0.6; }
.cc__send { display: grid; place-items: center; flex-shrink: 0; width: 40px; height: 40px; border: 0; border-radius: 50%; background: var(--nad-brand-600); color: #fff; cursor: pointer; transition: background 0.15s; }
.cc__send:hover:not(:disabled) { background: var(--nad-brand-700); }
.cc__send:disabled { opacity: 0.4; cursor: default; }
.cc__send:focus-visible { outline: 2px solid var(--nad-brand-600); outline-offset: 2px; }
.cc__count { margin: 4px 0 0; text-align: end; font-size: 12px; color: var(--nad-ink-soft); }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
</style>
