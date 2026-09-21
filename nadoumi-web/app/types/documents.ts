/** Lifecycle of a document, exactly as `nadoumi-document` reports it. */
export type DocumentLifecycle = 'DRAFT' | 'SUBMITTED' | 'IN_REVIEW' | 'VERIFIED' | 'REJECTED' | 'EXPIRED'

/** `GET /api/student/documents` version summary. */
export interface DocumentVersionDto {
  id: number
  versionNo: number
  contentType: string
  sizeBytes: number
  uploadedAt: string
  verificationStatus: string
  verifiedAt: string | null
}

/** `GET /api/student/documents`: a student's own document (no reviewer identity, no internal events). */
export interface StudentDocumentDto {
  id: number
  applicationId: number | null
  docType: string
  status: DocumentLifecycle
  expiresOn: string | null
  /** Only ever set while the document is REJECTED. */
  rejectionReason: string | null
  currentVersion: DocumentVersionDto | null
}

/** One entry of the configurable document-type dictionary. */
export interface DocumentTypeOption {
  value: string
  label: string
}

/** How a file is delivered: a signed URL for the browser to open, or bytes the backend streamed. */
export type DocumentFileAccess =
  | { kind: 'url', url: string }
  | { kind: 'blob', blob: Blob, filename: string }
