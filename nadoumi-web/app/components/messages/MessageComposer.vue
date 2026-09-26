<script setup lang="ts">
import { MESSAGE_ATTACHMENT_ACCEPT, MESSAGE_ATTACHMENT_MAX_MB, MESSAGE_ATTACHMENT_MIME } from '~/constants/messages'
import { MESSAGE_MAX_LENGTH } from '~/types/messages'
import { validateFile } from '~/utils/files'

/**
 * Textarea + attach + send. The parent owns the draft (v-model) and does the
 * sending; this component owns the attachment upload lifecycle (upload-then-attach,
 * so a slow upload never blocks typing) and hands the parent finished media ids.
 */
const props = defineProps<{ id: string, conversationId: number, busy?: boolean, disabled?: boolean }>()
const draft = defineModel<string>({ required: true })
const emit = defineEmits<{ submit: [attachmentMediaIds: number[]] }>()
const { t } = useI18n()
const { uploadAttachment } = useMessages()
const inputId = useId()

interface PendingAttachment { key: string, file: File, mediaId: number | null, uploading: boolean, error: string }
const pending = ref<PendingAttachment[]>([])
const pickError = ref('')

const uploading = computed(() => pending.value.some(p => p.uploading))
const readyAttachmentIds = computed(() => pending.value.filter(p => p.mediaId !== null).map(p => p.mediaId!))
const canSend = computed(() =>
  !props.busy && !props.disabled && !uploading.value
  && (draft.value.trim().length > 0 || readyAttachmentIds.value.length > 0))

async function addFile(file: File) {
  const problem = validateFile(file, { mime: MESSAGE_ATTACHMENT_MIME, maxMb: MESSAGE_ATTACHMENT_MAX_MB })
  if (problem) {
    pickError.value = problem === 'badType'
      ? t('dashboard.messages.attachmentBadType')
      : t('onboarding.upload.tooBig', { mb: MESSAGE_ATTACHMENT_MAX_MB })
    return
  }
  pickError.value = ''
  // reactive() here, not a plain object: mutating it below (item.mediaId = ...) must go
  // through the reactive proxy to notify canSend/readyAttachmentIds, or those computeds
  // never see the update and Send stays disabled forever after a successful upload.
  const item = reactive<PendingAttachment>({ key: `${file.name}-${file.size}-${Date.now()}`, file, mediaId: null, uploading: true, error: '' })
  pending.value.push(item)
  try {
    const { mediaId } = await uploadAttachment(props.conversationId, file)
    item.mediaId = mediaId
  }
  catch {
    item.error = t('dashboard.messages.attachmentUploadFailed')
  }
  finally {
    item.uploading = false
  }
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = '' // picking the same file again must still fire
  for (const file of files) void addFile(file)
}

function removePending(key: string) {
  pending.value = pending.value.filter(p => p.key !== key)
}

function submit() {
  if (!canSend.value) return
  emit('submit', readyAttachmentIds.value)
  pending.value = []
}
</script>

<template>
  <form class="grid gap-2" @submit.prevent="submit">
    <NAlert v-if="pickError" tone="danger">{{ pickError }}</NAlert>

    <ul v-if="pending.length" class="flex flex-wrap gap-2" data-test="pending-attachments">
      <li
        v-for="p in pending" :key="p.key"
        class="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 py-1 ps-2.5 pe-1.5 text-xs"
        :class="p.error ? 'border-red-300 bg-red-50' : ''"
        data-test="pending-attachment"
      >
        <span class="max-w-[10rem] truncate" :class="p.error ? 'text-red-700' : 'text-slate-700'">{{ p.file.name }}</span>
        <NSpinner v-if="p.uploading" class="h-3.5 w-3.5" />
        <button
          type="button"
          class="grid h-5 w-5 place-items-center rounded-full text-slate-400 hover:bg-slate-200 hover:text-slate-600"
          :aria-label="t('dashboard.messages.removeAttachment')"
          @click="removePending(p.key)"
        >
          ×
        </button>
      </li>
    </ul>

    <NTextarea
      :id="id"
      v-model="draft"
      :rows="3"
      :maxlength="MESSAGE_MAX_LENGTH"
      :disabled="disabled"
      :placeholder="t('dashboard.messages.composer')"
      @keydown.ctrl.enter.prevent="submit"
      @keydown.meta.enter.prevent="submit"
    />
    <div class="flex items-center justify-between">
      <label
        :for="inputId"
        class="inline-flex cursor-pointer items-center gap-1.5 rounded-md px-2 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
        :class="disabled ? 'pointer-events-none opacity-50' : ''"
        :title="t('dashboard.messages.attach')"
      >
        <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path d="M21.44 11.05 12.25 20.24a5 5 0 0 1-7.07-7.07l8.49-8.49a3.5 3.5 0 0 1 4.95 4.95l-8.49 8.49a2 2 0 0 1-2.83-2.83l7.78-7.78" />
        </svg>
        <span class="sr-only">{{ t('dashboard.messages.attach') }}</span>
        <input :id="inputId" type="file" multiple :accept="MESSAGE_ATTACHMENT_ACCEPT" :disabled="disabled" class="sr-only" @change="onPick">
      </label>
      <NButton type="submit" size="sm" :loading="busy" :disabled="!canSend">
        {{ t('dashboard.messages.send') }}
      </NButton>
    </div>
  </form>
</template>
