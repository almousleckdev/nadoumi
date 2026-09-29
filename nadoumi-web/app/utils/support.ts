import type { TicketStatus } from '~/types/support'

export type SupportBadgeTone = 'neutral' | 'brand' | 'success' | 'warning' | 'danger'

const STATUS_TONES: Record<TicketStatus, SupportBadgeTone> = {
  OPEN: 'brand',
  IN_PROGRESS: 'brand',
  WAITING_ON_STUDENT: 'warning',
  RESOLVED: 'success',
  CLOSED: 'neutral',
}

export function ticketStatusTone(status: TicketStatus): SupportBadgeTone {
  return STATUS_TONES[status] ?? 'neutral'
}

const REPLYABLE: ReadonlySet<TicketStatus> = new Set<TicketStatus>(['OPEN', 'IN_PROGRESS', 'WAITING_ON_STUDENT'])

/** Mirrors the backend rule: RESOLVED and CLOSED mean "done", the student opens a new ticket instead. */
export function canReplyToTicket(status: TicketStatus): boolean {
  return REPLYABLE.has(status)
}
