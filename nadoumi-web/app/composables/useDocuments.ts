import type { DocumentFileAccess, DocumentTypeOption, StudentDocumentDto } from '~/types/documents'

const FALLBACK_FILENAME = 'document'

function fileForm(file: File, fields: Record<string, string | number | undefined> = {}): FormData {
  const form = new FormData()
  for (const [key, value] of Object.entries(fields)) {
    if (value !== undefined) form.append(key, String(value))
  }
  form.append('file', file, file.name)
  return form
}

function filenameFrom(disposition: string | null): string {
  const match = disposition?.match(/filename="?([^";]+)"?/i)
  return match?.[1] ?? FALLBACK_FILENAME
}

/**
 * The signed-in student's documents (`/api/student/documents`, via the BFF proxy).
 * A download is either a short-lived signed URL, or for sensitive types the bytes
 * themselves: the browser never learns a storage location it was not handed.
 */
export function useDocuments() {
  const { studentFetch } = useApi()
  // See useApi.ts: useRequestFetch() forwards the incoming request's cookies during
  // SSR, which the global $fetch does not.
  const requestFetch = useRequestFetch()

  return {
    list: (applicantId: number, applicationId?: number) =>
      studentFetch<StudentDocumentDto[]>('documents', { query: { applicantId, applicationId } }),

    create: (applicantId: number, docType: string, file: File, applicationId?: number) =>
      studentFetch<StudentDocumentDto>('documents', {
        method: 'POST',
        body: fileForm(file, { applicantId, docType, applicationId }),
      }),

    /** Uploads a new version of an existing document. */
    replace: (id: number, file: File) =>
      studentFetch<StudentDocumentDto>(`documents/${id}/versions`, { method: 'POST', body: fileForm(file) }),

    remove: (id: number): Promise<void> => studentFetch<undefined>(`documents/${id}`, { method: 'DELETE' }),

    async fileAccess(id: number, versionNo: number): Promise<DocumentFileAccess> {
      // useRequestFetch()'s type doesn't declare `.raw`, but it is the same ofetch
      // instance as the global $fetch at runtime (Nuxt creates it via $fetch.create).
      const res = await (requestFetch as typeof $fetch).raw<Blob>(`/api/student/documents/${id}/versions/${versionNo}/content`, {
        query: { json: 1 },
        responseType: 'blob',
      })
      const contentType = res.headers.get('content-type') ?? ''
      const body = res._data as Blob
      if (contentType.includes('application/json')) {
        const parsed = JSON.parse(await body.text()) as { url: string }
        return { kind: 'url', url: parsed.url }
      }
      return { kind: 'blob', blob: body, filename: filenameFrom(res.headers.get('content-disposition')) }
    },

    /** The configurable type dictionary; students have no other way to learn the valid codes. */
    types: () => requestFetch<DocumentTypeOption[]>('/api/student-document-types'),
  }
}
