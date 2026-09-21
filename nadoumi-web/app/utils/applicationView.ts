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

export type StatusTone = 'neutral' | 'brand' | 'success' | 'warning' | 'danger'

export function statusTone(status: string | null): StatusTone {
  switch (status) {
    case 'CLOSED_SUCCESS': return 'success'
    case 'CLOSED_UNSUCCESSFUL': return 'danger'
    case 'CLOSED_WITHDRAWN': return 'neutral'
    case 'DRAFT': return 'warning'
    default: return 'brand'
  }
}

export const isClosed = (status: string | null): boolean => Boolean(status?.startsWith('CLOSED_'))
