import { documentContent } from '@/api/document'
import { safeHref } from '@/utils/url'

const JSON_TYPE = 'application/json'

/**
 * Opens one document version: a signed URL in a new tab, or (sensitive types) the
 * streamed bytes saved as a file. The backend picks the delivery, never the browser.
 */
export async function openDocumentFile(documentId: number, versionNo: number): Promise<void> {
  const blob = await documentContent(documentId, versionNo)
  if (blob.type.includes(JSON_TYPE)) {
    const { url } = JSON.parse(await blob.text()) as { url: string }
    const safe = safeHref(url)
    if (safe) window.open(safe, '_blank', 'noopener')
    return
  }
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = `document-${documentId}-v${versionNo}`
  link.click()
  URL.revokeObjectURL(objectUrl)
}
