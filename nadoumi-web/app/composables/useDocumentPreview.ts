import type { PreviewFile } from '~/utils/documents'
import { previewKind } from '~/utils/documents'

/** What is needed to look at a stored version: where it is and what it is. */
export interface PreviewableVersion {
  versionNo: number
  contentType: string
}

/**
 * Opens an uploaded PDF or picture in the document viewer. The file comes through the same access call as a download
 * (a signed URL or the streamed bytes), and a blob URL is released when the viewer closes or the page is left.
 */
export function useDocumentPreview() {
  const documents = useDocuments()
  const file = ref<PreviewFile | null>(null)

  const open = computed({
    get: () => file.value !== null,
    set: (value: boolean) => { if (!value) close() },
  })

  function close() {
    if (file.value?.src.startsWith('blob:')) URL.revokeObjectURL(file.value.src)
    file.value = null
  }

  /** True when the file was loaded and the viewer opened; false when the type cannot be shown. */
  async function show(title: string, documentId: number, version: PreviewableVersion): Promise<boolean> {
    const kind = previewKind(version.contentType)
    if (!kind) return false
    const access = await documents.fileAccess(documentId, version.versionNo)
    const src = access.kind === 'url' ? access.url : URL.createObjectURL(access.blob)
    close()
    file.value = { title, src, kind }
    return true
  }

  /** Opens a PDF that is not a stored document, such as the application form preview, from its bytes. */
  function showBlob(title: string, blob: Blob) {
    close()
    file.value = { title, src: URL.createObjectURL(blob), kind: 'pdf' }
  }

  onBeforeUnmount(close)

  return { file, open, show, showBlob, close }
}
