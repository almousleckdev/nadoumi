import { documentContent } from '@/api/document'
import { safeHref } from '@/utils/url'

const JSON_TYPE = 'application/json'

export type DocumentFileMode = 'view' | 'download'

function saveAs(blob: Blob, name: string) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name
  link.click()
  URL.revokeObjectURL(objectUrl)
}

/**
 * Opens one document version. `view` shows it in a new tab, `download` saves it as a file. The backend picks the
 * delivery (a signed link, or the bytes for sensitive types), never the browser.
 */
export async function openDocumentFile(documentId: number, versionNo: number, mode: DocumentFileMode = 'download'): Promise<void> {
  const blob = await documentContent(documentId, versionNo, mode === 'view')
  if (blob.type.includes(JSON_TYPE)) {
    const { url } = JSON.parse(await blob.text()) as { url: string }
    const safe = safeHref(url)
    if (!safe) return
    if (mode === 'view') {
      window.open(safe, '_blank', 'noopener')
      return
    }
    const link = document.createElement('a')
    link.href = safe
    link.rel = 'noopener'
    link.download = `document-${documentId}-v${versionNo}`
    link.click()
    return
  }
  if (mode === 'view') {
    window.open(URL.createObjectURL(blob), '_blank', 'noopener')
    return
  }
  saveAs(blob, `document-${documentId}-v${versionNo}`)
}
