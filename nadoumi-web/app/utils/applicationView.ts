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

const KNOWN_STATUSES = new Set([
  'DRAFT', 'IN_REVIEW', 'SUBMITTED_TO_UNIVERSITY', 'DECISION', 'OFFER', 'PRE_DEPARTURE',
  'CLOSED_SUCCESS', 'CLOSED_UNSUCCESSFUL', 'CLOSED_WITHDRAWN',
])

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
  'IN_REVIEW', 'SUBMITTED_TO_UNIVERSITY', 'DECISION', 'OFFER', 'PRE_DEPARTURE', 'CLOSED_SUCCESS',
] as const

/** Index of the step an application is on, or -1 when it is not on the path (draft, unsuccessful, unknown). */
export function trackerIndex(status: string | null): number {
  return TRACKER_STEPS.findIndex(step => step === status)
}

const IN_PROGRESS = new Set(['IN_REVIEW', 'SUBMITTED_TO_UNIVERSITY', 'DECISION'])
const OFFERED = new Set(['OFFER', 'PRE_DEPARTURE', 'CLOSED_SUCCESS'])

export interface ApplicationStats { submitted: number, inProgress: number, offers: number, drafts: number }

/** The headline numbers on the dashboard: what was sent, what is still moving, what turned into an offer. */
export function applicationStats(statuses: readonly (string | null)[]): ApplicationStats {
  return {
    submitted: statuses.filter(isSubmitted).length,
    inProgress: statuses.filter(s => s !== null && IN_PROGRESS.has(s)).length,
    offers: statuses.filter(s => s !== null && OFFERED.has(s)).length,
    drafts: statuses.filter(isDraft).length,
  }
}

export interface StageCount { status: string, count: number }

/** Submitted applications counted per status, in path order, empty stages left out. */
export function stageDistribution(statuses: readonly (string | null)[]): StageCount[] {
  const order = [...TRACKER_STEPS, 'CLOSED_UNSUCCESSFUL']
  return order
    .map(status => ({ status, count: statuses.filter(s => s === status).length }))
    .filter(entry => entry.count > 0)
}
