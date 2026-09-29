import { computed, reactive, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { uploadAttachment } from '@/api/conversation'

export const MAX_ATTACHMENTS = 5
const MAX_BYTES = 15 * 1024 * 1024
const ALLOWED_MIME = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/webp',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
])
const ALLOWED_EXTENSIONS = new Set(['doc', 'docx', 'pdf', 'jpg', 'jpeg', 'png', 'webp'])

export interface PendingAttachment {
  key: string
  file: File
  mediaId: number | null
  uploading: boolean
  error: string
}

function isAllowedType(file: File): boolean {
  if (!file.type) return true
  if (ALLOWED_MIME.has(file.type)) return true
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  return ALLOWED_EXTENSIONS.has(extension)
}

export function useMessageAttachments(conversationId: Ref<number | null>) {
  const { t } = useI18n()
  const pending = ref<PendingAttachment[]>([])

  const isUploading = computed(() => pending.value.some(p => p.uploading))
  const readyIds = computed(() => pending.value.flatMap(p => (p.mediaId === null ? [] : [p.mediaId])))

  async function upload(conversation: number, file: File) {
    const item = reactive<PendingAttachment>({
      key: `${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
      file,
      mediaId: null,
      uploading: true,
      error: '',
    })
    pending.value.push(item)
    try {
      const res = await uploadAttachment(conversation, file)
      item.mediaId = res.mediaId
    }
    catch {
      item.error = t('conversations.attachUploadFailed')
    }
    finally {
      item.uploading = false
    }
  }

  async function addFiles(files: File[]) {
    const conversation = conversationId.value
    if (!files.length || conversation === null) return

    const room = MAX_ATTACHMENTS - pending.value.length
    if (room <= 0) {
      ElMessage.warning(t('conversations.attachMaxCount'))
      return
    }
    if (files.length > room) ElMessage.warning(t('conversations.attachMaxCount'))

    for (const file of files.slice(0, room)) {
      if (file.size > MAX_BYTES) {
        ElMessage.warning(`${file.name}: ${t('conversations.attachTooBig')}`)
        continue
      }
      if (!isAllowedType(file)) {
        ElMessage.warning(t('conversations.attachInvalidType', { name: file.name }))
        continue
      }
      await upload(conversation, file)
    }
  }

  function remove(key: string) {
    pending.value = pending.value.filter(p => p.key !== key)
  }

  function clear() {
    pending.value = []
  }

  return { pending, isUploading, readyIds, addFiles, remove, clear }
}
