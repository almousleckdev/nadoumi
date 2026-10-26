/** Enumerations shared by the application screens; they mirror the backend's fixed vocabularies. */
export const APPLICATION_TYPES = ['PROGRAM_WITH_SCHOLARSHIP', 'PROGRAM_ONLY', 'SCHOLARSHIP_ONLY']

/** `nad_wf_stage.status_label` values of the seeded workflow definitions. */
// staff only receive submitted applications: drafts and withdrawn ones are never listed, so they are not filters
export const PIPELINE_STATUSES = [
  'SUBMITTED', 'UNDER_REVIEW', 'DOCUMENTS_REQUIRED', 'READY_FOR_UNIVERSITY_SUBMISSION', 'SUBMITTED_TO_UNIVERSITY',
  'UNDER_UNIVERSITY_REVIEW', 'INTERVIEW_REQUIRED', 'CONDITIONAL_OFFER', 'OFFER_RECEIVED', 'OFFER_ACCEPTED', 'WAITLISTED',
  'ADMISSION_CONFIRMED', 'CLOSED_SUCCESS', 'CLOSED_UNSUCCESSFUL',
]

/** Applications that started on the first workflow keep these codes until they close. */
export const LEGACY_STATUSES = ['IN_REVIEW', 'DECISION', 'OFFER', 'PRE_DEPARTURE']

export const STATUSES = [...PIPELINE_STATUSES, ...LEGACY_STATUSES]

/** Statuses where Nadoumi is still checking the application (the dashboard's "in review"). */
export const IN_REVIEW_STATUSES = ['SUBMITTED', 'UNDER_REVIEW', 'DOCUMENTS_REQUIRED', 'READY_FOR_UNIVERSITY_SUBMISSION', 'IN_REVIEW']

/** Statuses where the student holds an offer or has gone beyond it. */
export const OFFER_STATUSES = [
  'CONDITIONAL_OFFER', 'OFFER_RECEIVED', 'OFFER_ACCEPTED', 'ADMISSION_CONFIRMED', 'CLOSED_SUCCESS', 'OFFER', 'PRE_DEPARTURE',
]

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

export const STATUS_TONES: Record<string, Tone> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  UNDER_REVIEW: 'info',
  DOCUMENTS_REQUIRED: 'warning',
  READY_FOR_UNIVERSITY_SUBMISSION: 'info',
  UNDER_UNIVERSITY_REVIEW: 'info',
  INTERVIEW_REQUIRED: 'warning',
  CONDITIONAL_OFFER: 'success',
  OFFER_RECEIVED: 'success',
  OFFER_ACCEPTED: 'success',
  WAITLISTED: 'warning',
  ADMISSION_CONFIRMED: 'success',
  IN_REVIEW: 'info',
  SUBMITTED_TO_UNIVERSITY: 'info',
  DECISION: 'warning',
  OFFER: 'success',
  PRE_DEPARTURE: 'info',
  CLOSED_SUCCESS: 'success',
  CLOSED_UNSUCCESSFUL: 'danger',
  CLOSED_WITHDRAWN: 'neutral',
}

export const isClosedStatus = (status: string | null): boolean => Boolean(status?.startsWith('CLOSED_'))

/** Decision types the seeded workflow's guards look for; the field still accepts any value. */
export const DECISION_TYPES = [
  'NADOUMI_INTERNAL', 'DOCUMENTS_COMPLETE', 'FEE_SETTLED', 'UNIVERSITY_OFFER', 'SCHOLARSHIP_AWARD', 'APPLICANT_RESPONSE',
  'DOCUMENT_REVIEW', 'UNIVERSITY_REVIEW', 'OFFER_CONDITIONS', 'ADMISSION',
]

/** Steps whose note the student reads or the file keeps; the server refuses them without one. */
export const NOTE_REQUIRED_STEPS: readonly string[] = ['request_documents', 'ineligible', 'reject', 'mark_withdrawn']

/** The status that opens the Interview tab, and the statuses where the admission documents are handled. */
export const INTERVIEW_STATUS = 'INTERVIEW_REQUIRED'
export const ADMISSION_DOCUMENT_STATUSES: readonly string[] = ['ADMISSION_CONFIRMED', 'CLOSED_SUCCESS']
