import { documentContent } from '@/api/document'
import { saveBlob } from '@/utils/download'

export type DocumentFileMode = 'view' | 'download'

const GENERIC_TYPE = 'application/octet-stream'
const VIEW_URL_LIFETIME_MS = 60_000

const EXTENSIONS: Record<string, string> = {
  'application/pdf': 'pdf',
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
  'application/msword': 'doc',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'docx',
}

export function extensionFor(contentType: string | undefined): string {
  return EXTENSIONS[(contentType ?? '').split(';')[0].trim().toLowerCase()] ?? ''
}

/**
 * Opens one document version. `view` shows it in a new tab, `download` saves it as a file. Staff always receive the
 * bytes through the backend, so the file keeps its real type and gets a name with the right extension.
 */
export async function openDocumentFile(
  documentId: number,
  versionNo: number,
  mode: DocumentFileMode = 'download',
  contentType?: string,
): Promise<void> {
  const received = await documentContent(documentId, versionNo, mode === 'view')
  const type = received.type && received.type !== GENERIC_TYPE ? received.type : (contentType ?? received.type)
  const blob = type === received.type ? received : new Blob([received], { type })
  if (mode === 'view') {
    const objectUrl = URL.createObjectURL(blob)
    window.open(objectUrl, '_blank', 'noopener')
    setTimeout(() => URL.revokeObjectURL(objectUrl), VIEW_URL_LIFETIME_MS)
    return
  }
  const extension = extensionFor(type)
  saveBlob(blob, `document-${documentId}-v${versionNo}${extension ? `.${extension}` : ''}`)
}
