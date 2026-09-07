<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type UploadProps, type UploadRawFile } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { assetUrl } from '@/utils/asset'

/**
 * Uploads one image to a module media endpoint (e.g. `/api/staff/universities/12/logo`)
 * and binds the returned media id via `v-model`.
 *
 * Two modes:
 *  - bound (default): the parent record already has an id, so `action` is a real
 *    endpoint and the file is sent immediately.
 *  - deferred (`deferred` + `resolveAction`): the parent record is not saved yet.
 *    The file is held locally with an object-URL preview and only sent once the
 *    parent calls `flush(id)` after creating the record.
 */
const props = withDefaults(defineProps<{
  /** The stored media id, or null when nothing is attached yet. */
  modelValue: number | null
  /** Module upload path from the API root — e.g. `/api/staff/universities/12/logo`. */
  action: string
  /** Resolved URL to show when `modelValue` is set and no fresh upload response is held. */
  previewUrl?: string | null
  /** Square logo vs wide banner framing. */
  aspect?: 'square' | 'wide'
  maxMb?: number
  accept?: string
  /**
   * Hold the file locally instead of uploading now. The parent must call
   * `flush(id)` (exposed) once it has created the record.
   */
  deferred?: boolean
  /** Builds the real endpoint from a freshly created record id (deferred mode). */
  resolveAction?: (id: number | string) => string
  /** Read-only: show the current image but no dropzone (e.g. no edit permission). */
  disabled?: boolean
  /** Message shown in place of the dropzone while disabled with no image. */
  disabledHint?: string
}>(), {
  previewUrl: null,
  aspect: 'wide',
  maxMb: 5,
  accept: 'image/*',
  deferred: false,
  resolveAction: undefined,
  disabled: false,
  disabledHint: '',
})

const emit = defineEmits<{ 'update:modelValue': [v: number | null] }>()

const { t } = useI18n()

const uploadUrl = computed(() => {
  const base = (import.meta.env.VITE_APP_BASE_API || '/dev-api').replace(/\/$/, '')
  return `${base}${props.action}`
})
const headers = computed(() => ({ Authorization: `Bearer ${getToken() ?? ''}` }))

// URL from the most recent successful upload on this instance; takes precedence
// over `previewUrl` so the picture updates the moment an upload lands.
const freshUrl = ref<string | null>(null)
// Deferred mode: the not-yet-sent file plus its local preview URL.
const pendingRaw = ref<File | null>(null)
const pendingUrl = ref<string | null>(null)

const previewSrc = computed(() =>
  pendingUrl.value || assetUrl(freshUrl.value || props.previewUrl || ''))

function validate(raw: UploadRawFile): boolean {
  if (!raw.type.startsWith('image/')) {
    ElMessage.error(t('imageUpload.badType'))
    return false
  }
  if (raw.size / 1024 / 1024 > props.maxMb) {
    ElMessage.error(t('imageUpload.tooBig', { mb: props.maxMb }))
    return false
  }
  return true
}

const beforeUpload: UploadProps['beforeUpload'] = (raw: UploadRawFile) => {
  if (!validate(raw)) return false
  if (props.deferred) {
    // hold it — nothing goes over the wire until the parent flushes
    revokePending()
    pendingRaw.value = raw
    pendingUrl.value = URL.createObjectURL(raw)
    return false
  }
  return true
}

/** Nadoumi module endpoints return the record raw: `{ mediaId, url? }`. */
interface UploadResult {
  mediaId?: number
  url?: string | null
  // problem+json fields, surfaced defensively if a 2xx ever carries them
  msg?: string
  detail?: string
  title?: string
}
function onSuccess(res: UploadResult) {
  if (typeof res?.mediaId !== 'number') {
    ElMessage.error(res?.detail || res?.title || res?.msg || t('imageUpload.failed'))
    return
  }
  freshUrl.value = res.url ?? null
  emit('update:modelValue', res.mediaId)
}

/** el-upload hands `onError` an Error whose `message` is the raw response body. */
function onError(err: Error & { status?: number }) {
  let message = ''
  try {
    const body = JSON.parse(err?.message ?? '') as UploadResult
    message = body.detail || body.title || body.msg || ''
  }
  catch {
    // not JSON — fall through to a status-based message
  }
  if (!message) {
    if (err?.status === 413) message = t('imageUpload.tooBig', { mb: props.maxMb })
    else if (err?.status === 415 || err?.status === 422) message = t('imageUpload.badType')
    else message = t('imageUpload.failed')
  }
  ElMessage.error(message)
}

function revokePending() {
  if (pendingUrl.value) URL.revokeObjectURL(pendingUrl.value)
  pendingUrl.value = null
}

function clear() {
  revokePending()
  pendingRaw.value = null
  freshUrl.value = null
  emit('update:modelValue', null)
}

function hasPending(): boolean {
  return pendingRaw.value != null
}

/**
 * Send a deferred file now that the parent record exists. Errors are surfaced as
 * a toast but never rejected — the record is already saved, the image is a
 * secondary concern the user can retry from the edit view.
 */
async function flush(id: number | string): Promise<void> {
  if (!pendingRaw.value) return
  const target = props.resolveAction ? props.resolveAction(id) : props.action
  const fd = new FormData()
  fd.append('file', pendingRaw.value)
  try {
    const res = await request.post<UploadResult>(target, fd)
    const body = res.data
    if (typeof body?.mediaId === 'number') {
      freshUrl.value = body.url ?? null
      emit('update:modelValue', body.mediaId)
    }
    revokePending()
    pendingRaw.value = null
  }
  catch {
    // request.ts already toasts the problem+json detail
  }
}

onBeforeUnmount(revokePending)

defineExpose({ onSuccess, onError, clear, flush, hasPending })
</script>

<template>
  <div
    class="img-upload"
    :class="aspect"
  >
    <div
      v-if="previewSrc"
      class="img-upload__preview"
    >
      <img
        :src="previewSrc"
        alt=""
      >
      <button
        v-if="!disabled"
        type="button"
        class="img-upload__remove"
        :title="t('common.delete')"
        @click="clear"
      >
        <el-icon><Delete /></el-icon>
      </button>
    </div>
    <div
      v-else-if="disabled"
      class="img-upload__disabled"
    >
      {{ disabledHint }}
    </div>
    <el-upload
      v-else
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      name="file"
      :accept="accept"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      drag
      class="img-upload__drop"
    >
      <el-icon class="img-upload__icon">
        <Plus />
      </el-icon>
      <div class="img-upload__hint">
        {{ t('imageUpload.choose') }}
      </div>
    </el-upload>
  </div>
</template>

<style scoped>
.img-upload__preview {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--nad-border, #e5e7eb);
  width: fit-content;
}
.img-upload__preview img {
  display: block;
  object-fit: cover;
  background: #f4f4f5;
}
.square .img-upload__preview img,
.square :deep(.el-upload-dragger) {
  width: 120px;
  height: 120px;
}
.wide .img-upload__preview img,
.wide :deep(.el-upload-dragger) {
  width: 280px;
  height: 140px;
}
.img-upload__disabled {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 12px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--nad-ink-soft, #6b7280);
  border: 1px dashed var(--nad-border, #e5e7eb);
  border-radius: 8px;
}
.square .img-upload__disabled { width: 120px; height: 120px; }
.wide .img-upload__disabled { width: 280px; height: 140px; }
.img-upload__remove {
  position: absolute;
  top: 6px;
  right: 6px;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 6px;
  padding: 4px 6px;
  cursor: pointer;
  display: inline-flex;
}
.img-upload :deep(.el-upload-dragger) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0;
}
.img-upload__icon {
  font-size: 22px;
  color: var(--nad-ink-faint, #9ca3af);
}
.img-upload__hint {
  font-size: 12px;
  color: var(--nad-ink-soft, #6b7280);
}
</style>
