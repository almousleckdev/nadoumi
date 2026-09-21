import { describe, it, expect } from 'vitest'
import { TICKET_STATUSES } from '@/api/support'
import { allowedNextStatuses, canStaffReply } from '@/views/support/ticketWorkflow'

describe('ticket workflow (mirrors TicketStatus.allowedNext on the backend)', () => {
  it.each([
    ['OPEN', ['IN_PROGRESS']],
    ['IN_PROGRESS', ['WAITING_ON_STUDENT', 'RESOLVED']],
    ['WAITING_ON_STUDENT', ['IN_PROGRESS', 'RESOLVED']],
    ['RESOLVED', ['CLOSED']],
    ['CLOSED', []],
  ] as const)('%s can move to %j', (from, to) => {
    expect([...allowedNextStatuses(from)].sort()).toEqual([...to].sort())
  })

  it('never lets a ticket skip straight to CLOSED or reopen', () => {
    for (const s of TICKET_STATUSES) {
      if (s !== 'RESOLVED') expect(allowedNextStatuses(s)).not.toContain('CLOSED')
      expect(allowedNextStatuses(s)).not.toContain('OPEN')
    }
  })

  it('lets staff reply until the ticket is closed', () => {
    expect(canStaffReply('RESOLVED')).toBe(true)
    expect(canStaffReply('CLOSED')).toBe(false)
  })
})
