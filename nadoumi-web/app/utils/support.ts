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

/**
 * Safely formats a ticket timestamp.
 * Handles ISO-8601 strings and MySQL/Jackson 'YYYY-MM-DD HH:mm:ss' formatted strings cross-browser.
 */
export function formatSupportDate(
  iso: string | null | undefined,
  locale: string,
  options: Intl.DateTimeFormatOptions = { dateStyle: 'medium' },
): string {
  if (!iso) return ''
  const normalized = typeof iso === 'string' && iso.includes(' ') && !iso.includes('T')
    ? iso.replace(' ', 'T')
    : iso
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return ''
  try {
    return new Intl.DateTimeFormat(locale, options).format(date)
  }
  catch {
    return ''
  }
}
