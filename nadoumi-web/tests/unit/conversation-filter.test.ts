import { describe, it, expect } from 'vitest'
import { filterConversations } from '~/utils/messages'
import type { ConversationSummary } from '~/types/messages'

const C = (over: Partial<ConversationSummary>): ConversationSummary => ({
  id: 1, subject: 'Visa question', applicationId: null, conversationType: 'GENERAL', status: 'OPEN',
  lastMessagePreview: 'Please send your passport.', lastMessageAt: '2026-01-01T10:00:00', unreadCount: 0,
  adminName: 'Amina', ...over,
})

const items = [
  C({ id: 1 }),
  C({ id: 2, subject: 'Scholarship deadline', adminName: 'Karim', lastMessagePreview: 'Closes in April', applicationId: 4021 }),
  C({ id: 3, subject: null as unknown as string, adminName: null as unknown as string, lastMessagePreview: null as unknown as string }),
]

describe('filterConversations', () => {
  it('returns every conversation for an empty or blank query', () => {
    expect(filterConversations(items, '')).toHaveLength(3)
    expect(filterConversations(items, '   ')).toHaveLength(3)
  })

  it('matches the subject, ignoring case', () => {
    expect(filterConversations(items, 'SCHOLARSHIP').map(c => c.id)).toEqual([2])
  })

  it('matches the advisor name', () => {
    expect(filterConversations(items, 'amina').map(c => c.id)).toEqual([1])
  })

  it('matches the last message preview', () => {
    expect(filterConversations(items, 'passport').map(c => c.id)).toEqual([1])
  })

  it('matches the application id', () => {
    expect(filterConversations(items, '4021').map(c => c.id)).toEqual([2])
  })

  it('trims the query and copes with missing fields', () => {
    expect(filterConversations(items, '  april ').map(c => c.id)).toEqual([2])
    expect(filterConversations(items, 'nothing matches')).toEqual([])
  })
})
