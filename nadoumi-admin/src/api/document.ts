import request from '@/utils/request'

/** Lifecycle of a document, exactly as `nadoumi-document` reports it. */
export type DocumentLifecycle = 'DRAFT' | 'SUBMITTED' | 'IN_REVIEW' | 'VERIFIED' | 'REJECTED' | 'EXPIRED'

export interface DocumentVersion {
  id: number
  versionNo: number
  contentType: string
  sizeBytes: number
  uploadedAt: string
  verificationStatus: string
  verifiedAt: string | null
}

export interface DocumentEvent {
  id: number
  eventType: string
  actorUserId: number
  at: string
  /** Free text; the rejection reason for a REJECTED event. */
  detail: string | null
}

/** `GET /api/staff/documents`: the full staff view (reviewer, every version, the audit log). */
export interface StaffDocument {
  id: number
  applicantId: number
  applicationId: number | null
  docType: string
  status: DocumentLifecycle
  expiresOn: string | null
  reviewerUserId: number | null
  rejectionReason: string | null
  versions: DocumentVersion[]
  events: DocumentEvent[]
}

export interface DocumentTypeEntry {
  dictLabel: string
  dictValue: string
}

/** RuoYi dictionary that holds the configurable document types. */
export const DOCUMENT_TYPE_DICT = 'nad_document_type'

// ---- staff review (nad:document:view / download / verify / reject) ----
export const listStaffDocuments = (params: { applicantId?: number, applicationId?: number } = {}) =>
  request.get<unknown, StaffDocument[]>('/api/staff/documents', { params })

export const getStaffDocument = (id: number) =>
  request.get<unknown, StaffDocument>(`/api/staff/documents/${id}`)

export const verifyDocument = (id: number) =>
  request.post<unknown, void>(`/api/staff/documents/${id}/verify`)

export const rejectDocument = (id: number, reason: string) =>
  request.post<unknown, void>(`/api/staff/documents/${id}/reject`, { reason })

/**
 * The file of one version: a JSON `{ url }` (signed URL) for ordinary types, or the
 * streamed bytes for sensitive ones. Both arrive as a Blob; the caller inspects its type.
 */
export const documentContent = (id: number, versionNo: number) =>
  request.get<unknown, Blob>(`/api/staff/documents/${id}/versions/${versionNo}/content`, {
    params: { json: 1 },
    responseType: 'blob',
  })

/** The document-type dictionary (RuoYi `AjaxResult`: `{ code, msg, data }`). */
export const listDocumentTypes = () =>
  request.get<unknown, { data: DocumentTypeEntry[] }>(`/system/dict/data/type/${DOCUMENT_TYPE_DICT}`)
