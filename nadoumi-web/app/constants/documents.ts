import type { DocumentLifecycle } from '~/types/documents'

/** Mirrors the backend upload policy for applicant documents (PDF/JPEG/PNG, 20 MB); the server still enforces it. */
export const DOCUMENT_MAX_MB = 20
export const DOCUMENT_ACCEPT = 'application/pdf,image/jpeg,image/png'
export const DOCUMENT_MIME = /^(application\/pdf|image\/(jpeg|png))$/

/** Badge tone per lifecycle state. */
export const DOCUMENT_LIFECYCLE_TONES: Record<DocumentLifecycle, 'neutral' | 'brand' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'brand',
  IN_REVIEW: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger',
  EXPIRED: 'danger',
}

/** Only a document that never received a file can be deleted (`DocumentService.deleteDraft`). */
export function canDeleteDocument(status: DocumentLifecycle): boolean {
  return status === 'DRAFT'
}
