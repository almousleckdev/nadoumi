/**
 * Student-facing projection helpers for `/api/student/applications`.
 *
 * The server returns raw event-type codes in `timeline` (including internal ones such
 * as case assignment or task events). A student only ever sees a fixed, translated
 * vocabulary: known milestones get their own label, every other stage change is a
 * neutral "moved to the next stage", and internal events are dropped. Unknown
 * codes are never rendered verbatim.
 */
export type TimelineKey = 'created' | 'submitted' | 'withdrawn' | 'offerAccepted' | 'offerDeclined' | 'stageChanged'

const MILESTONES: Record<string, TimelineKey> = {
  APPLICATION_CREATED: 'created',
  STAGE_SUBMIT: 'submitted',
  STAGE_WITHDRAW: 'withdrawn',
  STAGE_ACCEPT_OFFER: 'offerAccepted',
  STAGE_DECLINE_OFFER: 'offerDeclined',
}

const STAGE_EVENT_PREFIX = 'STAGE_'

/** Maps raw event codes to the student-safe timeline keys, dropping anything internal. */
export function safeTimeline(events: readonly string[]): TimelineKey[] {
  const out: TimelineKey[] = []
  for (const code of events) {
    const milestone = MILESTONES[code]
    if (milestone) out.push(milestone)
    else if (code.startsWith(STAGE_EVENT_PREFIX)) out.push('stageChanged')
  }
  return out
}

/** Every status the UI has copy for, in the order a case moves through them (the first workflow's codes last). */
const STATUS_ORDER = [
  'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'DOCUMENTS_REQUIRED', 'READY_FOR_UNIVERSITY_SUBMISSION', 'SUBMITTED_TO_UNIVERSITY',
  'UNDER_UNIVERSITY_REVIEW', 'INTERVIEW_REQUIRED', 'WAITLISTED', 'CONDITIONAL_OFFER', 'OFFER_RECEIVED', 'OFFER_ACCEPTED',
  'ADMISSION_CONFIRMED', 'CLOSED_SUCCESS', 'CLOSED_UNSUCCESSFUL', 'CLOSED_WITHDRAWN',
  'IN_REVIEW', 'DECISION', 'OFFER', 'PRE_DEPARTURE',
] as const

const KNOWN_STATUSES: ReadonlySet<string> = new Set(STATUS_ORDER)

/** The i18n suffix for a status label, or `unknown` for anything the UI has no copy for. */
export function statusKey(status: string | null): string {
  return status && KNOWN_STATUSES.has(status) ? status : 'unknown'
}

export type StatusTone = 'neutral' | 'danger'

/** Chips are neutral (CLAUDE.md §24); only an unsuccessful outcome reads as an error. */
export function statusTone(status: string | null): StatusTone {
  return status === 'CLOSED_UNSUCCESSFUL' ? 'danger' : 'neutral'
}

/** The applications a student sees in their list: paid for and sent. Drafts are finished in the apply flow. */
export const isSubmitted = (status: string | null): boolean => status !== 'DRAFT' && status !== 'CLOSED_WITHDRAWN'

export const isDraft = (status: string | null): boolean => status === 'DRAFT'

export const isClosed = (status: string | null): boolean => Boolean(status?.startsWith('CLOSED_'))

/** The path a submitted application follows, in order; the tracker marks how far it has got. */
export const TRACKER_STEPS = [
  'SUBMITTED', 'UNDER_REVIEW', 'SUBMITTED_TO_UNIVERSITY', 'UNDER_UNIVERSITY_REVIEW', 'OFFER_RECEIVED', 'OFFER_ACCEPTED',
  'ADMISSION_CONFIRMED', 'CLOSED_SUCCESS',
] as const

/** Where each status sits on the path. Side states (documents, interview, waitlist) hold the step they interrupt. */
const TRACKER_POSITION: Record<string, number> = {
  SUBMITTED: 0,
  UNDER_REVIEW: 1, DOCUMENTS_REQUIRED: 1, READY_FOR_UNIVERSITY_SUBMISSION: 1, IN_REVIEW: 1,
  SUBMITTED_TO_UNIVERSITY: 2,
  UNDER_UNIVERSITY_REVIEW: 3, INTERVIEW_REQUIRED: 3, WAITLISTED: 3, DECISION: 3,
  OFFER_RECEIVED: 4, CONDITIONAL_OFFER: 4, OFFER: 4,
  OFFER_ACCEPTED: 5,
  ADMISSION_CONFIRMED: 6, PRE_DEPARTURE: 6,
  CLOSED_SUCCESS: 7,
}

/** Index of the step an application is on, or -1 when it is not on the path (draft, closed unsuccessfully, unknown). */
export function trackerIndex(status: string | null): number {
  return status ? (TRACKER_POSITION[status] ?? -1) : -1
}

const OFFERED = new Set([
  'OFFER_RECEIVED', 'CONDITIONAL_OFFER', 'OFFER_ACCEPTED', 'ADMISSION_CONFIRMED', 'CLOSED_SUCCESS', 'OFFER', 'PRE_DEPARTURE',
])

export interface ApplicationStats { submitted: number, inProgress: number, offers: number, drafts: number }

/** The headline numbers on the dashboard: what was sent, what is still moving, what turned into an offer. */
export function applicationStats(statuses: readonly (string | null)[]): ApplicationStats {
  const submitted = statuses.filter(isSubmitted)
  const offers = statuses.filter(s => s !== null && OFFERED.has(s)).length
  return {
    submitted: submitted.length,
    inProgress: submitted.filter(s => s !== null && !isClosed(s) && !OFFERED.has(s)).length,
    offers,
    drafts: statuses.filter(isDraft).length,
  }
}

export interface StageCount { status: string, count: number }

/** Submitted applications counted per status, in path order, empty statuses left out. */
export function stageDistribution(statuses: readonly (string | null)[]): StageCount[] {
  return STATUS_ORDER
    .filter(status => isSubmitted(status))
    .map(status => ({ status: status as string, count: statuses.filter(s => s === status).length }))
    .filter(entry => entry.count > 0)
}
