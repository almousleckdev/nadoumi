import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useSupport } from '~/composables/useSupport'
import { canReplyToTicket, ticketStatusTone } from '~/utils/support'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)

beforeEach(() => fetchImpl.mockReset().mockResolvedValue(undefined))

describe('useSupport', () => {
  it('lists own tickets through the student BFF with the status filter and paging', async () => {
    await useSupport().list({ status: 'OPEN', page: 1, size: 20 })
    expect(fetchImpl).toHaveBeenCalledWith('/api/student/support/tickets', { query: { status: 'OPEN', page: 1, size: 20 } })
  })

  it('loads one ticket', async () => {
    await useSupport().get(7)
    expect(fetchImpl).toHaveBeenCalledWith('/api/student/support/tickets/7')
  })

  it('creates a ticket with only the fields the API takes', async () => {
    const body = { subject: 'Visa', category: 'APPLICATION' as const, body: 'Help please' }
    await useSupport().create(body)
    expect(fetchImpl).toHaveBeenCalledWith('/api/student/support/tickets', { method: 'POST', body })
  })

  it('replies on the ticket messages endpoint', async () => {
    await useSupport().reply(7, 'thanks')
    expect(fetchImpl).toHaveBeenCalledWith('/api/student/support/tickets/7/messages', { method: 'POST', body: { body: 'thanks' } })
  })
})

describe('support status helpers', () => {
  it('mirrors the backend: replies only while open, in progress or waiting on the student', () => {
    expect(canReplyToTicket('OPEN')).toBe(true)
    expect(canReplyToTicket('IN_PROGRESS')).toBe(true)
    expect(canReplyToTicket('WAITING_ON_STUDENT')).toBe(true)
    expect(canReplyToTicket('RESOLVED')).toBe(false)
    expect(canReplyToTicket('CLOSED')).toBe(false)
  })

  it('asks for attention when the ticket waits on the student', () => {
    expect(ticketStatusTone('WAITING_ON_STUDENT')).toBe('warning')
    expect(ticketStatusTone('RESOLVED')).toBe('success')
  })
})
