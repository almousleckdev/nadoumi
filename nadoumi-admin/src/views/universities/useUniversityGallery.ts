import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import type { DeferredTask } from '@/composables/useDeferredImages'
import { MAX_GALLERY, type GalleryRow, type UniversityForm } from './universityForm'

const MAX_IMAGE_MB = 5

interface GalleryUploadResult { mediaId?: number, url?: string | null }

async function uploadGalleryFile(universityId: number, file: File): Promise<GalleryUploadResult | null> {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const { data } = await request.post<GalleryUploadResult>(`/api/staff/universities/${universityId}/gallery`, fd)
    return typeof data?.mediaId === 'number' ? data : null
  }
  catch {
    return null
  }
}

async function uploadHeldRow(universityId: number, row: GalleryRow): Promise<boolean> {
  if (!row._file) return true
  const res = await uploadGalleryFile(universityId, row._file)
  if (!res) return false
  row.mediaId = res.mediaId ?? null
  row.imageUrl = res.url ?? null
  if (row.url) URL.revokeObjectURL(row.url)
  row.url = res.url ?? null
  row._file = null
  return true
}

export function useUniversityGallery(form: UniversityForm) {
  const { t } = useI18n()

  async function addFiles(files: File[]) {
    const room = MAX_GALLERY - form.gallery.length
    if (room <= 0 || !files.length) return
    if (files.length > room) ElMessage.warning(t('university.galleryTrimmed', { n: MAX_GALLERY }))
    for (const file of files.slice(0, room)) {
      if (!file.type.startsWith('image/')) {
        ElMessage.error(t('imageUpload.badType'))
        continue
      }
      if (file.size / 1024 / 1024 > MAX_IMAGE_MB) {
        ElMessage.error(t('imageUpload.tooBig', { mb: MAX_IMAGE_MB }))
        continue
      }
      if (form.id) {
        const res = await uploadGalleryFile(form.id, file)
        if (res) form.gallery.push({ imageUrl: res.url ?? null, mediaId: res.mediaId ?? null, url: res.url ?? null, caption: null })
      }
      else {
        form.gallery.push({ imageUrl: null, mediaId: null, url: URL.createObjectURL(file), caption: null, _file: file })
      }
    }
  }

  function removeRow(index: number) {
    const row = form.gallery[index]
    if (row?._file && row.url) URL.revokeObjectURL(row.url)
    form.gallery.splice(index, 1)
  }

  function heldUploadTasks(universityId: number): DeferredTask[] {
    return form.gallery.filter(row => row._file).map(row => () => uploadHeldRow(universityId, row))
  }

  return { addFiles, removeRow, heldUploadTasks }
}
