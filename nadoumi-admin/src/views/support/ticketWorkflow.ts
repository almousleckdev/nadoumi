import type { TicketPriority, TicketStatus } from '@/api/support'

/**
 * Mirrors TicketStatus.allowedNext() on the backend:
 * OPEN -> IN_PROGRESS -> {WAITING_ON_STUDENT <-> IN_PROGRESS} -> RESOLVED -> CLOSED.
 * CLOSED is terminal. The backend enforces this too; the UI only offers moves that will succeed.
 */
const NEXT: Record<TicketStatus, readonly TicketStatus[]> = {
  OPEN: ['IN_PROGRESS'],
  IN_PROGRESS: ['WAITING_ON_STUDENT', 'RESOLVED'],
  WAITING_ON_STUDENT: ['IN_PROGRESS', 'RESOLVED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
}

export function allowedNextStatuses(status: TicketStatus): readonly TicketStatus[] {
  return NEXT[status] ?? []
}

/** Mirrors the student reply rule: staff can still answer until the ticket is CLOSED. */
export function canStaffReply(status: TicketStatus): boolean {
  return status !== 'CLOSED'
}

/** Vocabulary of the shared StatusBadge. */
export function statusTone(status: TicketStatus): string {
  switch (status) {
    case 'OPEN': return 'PENDING'
    case 'IN_PROGRESS': return 'IN_REVIEW'
    case 'WAITING_ON_STUDENT': return 'SUBMITTED'
    case 'RESOLVED': return 'ACTIVE'
    default: return 'DRAFT'
  }
}

export function priorityTone(priority: TicketPriority): string {
  switch (priority) {
    case 'URGENT': return 'FAILED'
    case 'HIGH': return 'PENDING'
    case 'NORMAL': return 'SUBMITTED'
    default: return 'DRAFT'
  }
}
