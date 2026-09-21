/** Enumerations shared by the application screens; they mirror the backend's fixed vocabularies. */
export const APPLICATION_TYPES = ['PROGRAM_WITH_SCHOLARSHIP', 'PROGRAM_ONLY']

/** `nad_wf_stage.status_label` values of the seeded workflow definitions. */
export const STATUSES = [
  'DRAFT', 'IN_REVIEW', 'SUBMITTED_TO_UNIVERSITY', 'DECISION', 'OFFER', 'PRE_DEPARTURE',
  'CLOSED_SUCCESS', 'CLOSED_UNSUCCESSFUL', 'CLOSED_WITHDRAWN',
]

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

export const STATUS_TONES: Record<string, Tone> = {
  DRAFT: 'neutral',
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
]
