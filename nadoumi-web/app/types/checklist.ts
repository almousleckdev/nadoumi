import type { DocumentLifecycle } from '~/types/documents'

/** `GET /api/student/applications/{id}/documents/checklist`: one row per document the application needs. */
export interface ChecklistItemDto {
  docType: string
  mandatory: boolean
  note: string | null
  /** `MISSING` until the student has uploaded a document of that type. */
  status: 'MISSING' | DocumentLifecycle
  documentId: number | null
  /** Only set while the document is REJECTED. */
  rejectionReason: string | null
}
