import { describe, it, expect } from 'vitest'
import { formatMessageTime, mergeMessages } from '@/utils/messages'
import type { ConversationMessage } from '@/api/conversation'

const M = (id: number, body = `m${id}`): ConversationMessage => ({
  id, conversationId: 9, senderUserId: 1, senderName: 'Ada', body,
  createdAt: '2026-01-01T10:00:00', editedAt: null, attachments: [],
})

describe('mergeMessages', () => {
  it('orders oldest first regardless of the newest-first page order', () => {
    expect(mergeMessages([], [M(3), M(2), M(1)]).map(m => m.id)).toEqual([1, 2, 3])
  })

  it('never duplicates a message already shown', () => {
    expect(mergeMessages([M(1), M(2)], [M(3), M(2)]).map(m => m.id)).toEqual([1, 2, 3])
  })

  it('prefers the incoming copy so an edit replaces the stale body', () => {
    expect(mergeMessages([M(1, 'old')], [M(1, 'new')])[0]!.body).toBe('new')
  })
})

describe('formatMessageTime', () => {
  const now = new Date('2026-06-15T12:00:00')

  it('shows only the time for today and a date for older messages', () => {
    expect(formatMessageTime('2026-06-15T09:30:00', 'en', now)).not.toMatch(/2026/)
    expect(formatMessageTime('2026-01-02T09:30:00', 'en', now)).toMatch(/2026/)
  })

  it('returns an empty string for missing or invalid input', () => {
    expect(formatMessageTime(null, 'en', now)).toBe('')
    expect(formatMessageTime('nope', 'en', now)).toBe('')
  })
})
