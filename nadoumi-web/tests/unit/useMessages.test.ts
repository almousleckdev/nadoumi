import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useMessages } from '~/composables/useMessages'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

beforeEach(() => studentFetch.mockReset())

describe('useMessages', () => {
  it('GET conversations', async () => {
    studentFetch.mockResolvedValueOnce([{ id: 1 }])
    const rows = await useMessages().listConversations()
    expect(studentFetch).toHaveBeenCalledWith('conversations')
    expect(rows).toEqual([{ id: 1 }])
  })

  it('pages messages backwards with the beforeId cursor (0 = newest page)', async () => {
    studentFetch.mockResolvedValue([])
    await useMessages().listMessages(9)
    await useMessages().listMessages(9, 41)
    expect(studentFetch).toHaveBeenNthCalledWith(1, 'conversations/9/messages', { query: { beforeId: 0 } })
    expect(studentFetch).toHaveBeenNthCalledWith(2, 'conversations/9/messages', { query: { beforeId: 41 } })
  })

  it('POSTs a message body only', async () => {
    studentFetch.mockResolvedValueOnce({ id: 5 })
    await useMessages().post(9, 'hello')
    expect(studentFetch).toHaveBeenCalledWith('conversations/9/messages', { method: 'POST', body: { body: 'hello' } })
  })

  it('opens a conversation for an applicant', async () => {
    studentFetch.mockResolvedValueOnce({ id: 1, conversationId: 12 })
    await useMessages().open({ applicantId: 3, subject: 'Visa', body: 'Question' })
    expect(studentFetch).toHaveBeenCalledWith('conversations', {
      method: 'POST', body: { applicantId: 3, subject: 'Visa', body: 'Question' },
    })
  })

  it('marks a conversation read', async () => {
    studentFetch.mockResolvedValueOnce(undefined)
    await useMessages().markRead(9)
    expect(studentFetch).toHaveBeenCalledWith('conversations/9/read', { method: 'POST' })
  })
})
