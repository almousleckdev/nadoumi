import { describe, it, expect } from 'vitest'
import type { ChatConversation, ChatMessage } from '@/api/conversation'
import {
  clockTime, dayLabel, fileKind, filterInbox, formatBytes, groupByDay, inboxTime, maxPointer, mergeMessages,
  receiptStatus, relativeTime, sameCluster, sortInbox,
} from '@/utils/chat'

const M = (id: number, over: Partial<ChatMessage> = {}): ChatMessage => ({
  id, conversationId: 1, senderUserId: 7, senderName: 'x', body: `m${id}`, createdAt: '2026-10-09T08:00:00',
  editedAt: null, attachments: [], ...over,
})
const C = (id: number, over: Partial<ChatConversation> = {}): ChatConversation => ({
  id, subject: null, applicationId: null, conversationType: 'DIRECT', status: 'OPEN', lastMessageId: null,
  lastMessagePreview: null, lastMessageAt: null, lastSenderUserId: null, unreadCount: 0,
  peer: { userId: 2, name: 'Jane', avatarUrl: null, online: false, lastSeenAt: null },
  peerDeliveredMessageId: null, peerReadMessageId: null, ...over,
})

describe('mergeMessages', () => {
  it('unites by id, newest copy wins, oldest first', () => {
    const merged = mergeMessages([M(3), M(1)], [M(2), M(3, { body: 'edited' })])
    expect(merged.map(m => m.id)).toEqual([1, 2, 3])
    expect(merged[2]!.body).toBe('edited')
  })

  it('keeps unconfirmed (negative id) messages last, in send order', () => {
    const merged = mergeMessages([M(5), M(-2), M(-1)], [M(6)])
    expect(merged.map(m => m.id)).toEqual([5, 6, -1, -2])
  })
})

describe('receiptStatus', () => {
  const conv = { peerDeliveredMessageId: 4, peerReadMessageId: 2 }

  it('is read, delivered or sent according to the other side\'s pointers', () => {
    expect(receiptStatus(M(2), conv)).toBe('read')
    expect(receiptStatus(M(3), conv)).toBe('delivered')
    expect(receiptStatus(M(5), conv)).toBe('sent')
    expect(receiptStatus(M(1), null)).toBe('sent')
  })

  it('reports sending and failed above anything else', () => {
    expect(receiptStatus(M(-1, { sendState: 'sending' }), conv)).toBe('sending')
    expect(receiptStatus(M(-1, { sendState: 'failed' }), conv)).toBe('failed')
  })
})

describe('pointers and ordering', () => {
  it('only ever moves a receipt pointer forward', () => {
    expect(maxPointer(null, 5)).toBe(5)
    expect(maxPointer(9, 5)).toBe(9)
    expect(maxPointer(5, 9)).toBe(9)
  })

  it('orders the inbox by latest activity, newest first', () => {
    const sorted = sortInbox([C(1, { lastMessageAt: '2026-10-08T10:00:00' }), C(2, { lastMessageAt: '2026-10-09T10:00:00' }), C(3)])
    expect(sorted.map(c => c.id)).toEqual([2, 1, 3])
  })
})

describe('grouping', () => {
  it('splits messages into calendar days', () => {
    const groups = groupByDay([M(1, { createdAt: '2026-10-08T23:59:00' }), M(2, { createdAt: '2026-10-09T00:01:00' }), M(3, { createdAt: '2026-10-09T09:00:00' })])
    expect(groups.map(g => g.messages.length)).toEqual([1, 2])
  })

  it('clusters consecutive messages from one sender within five minutes', () => {
    const a = M(1, { createdAt: '2026-10-09T08:00:00' })
    expect(sameCluster(a, M(2, { createdAt: '2026-10-09T08:04:00' }))).toBe(true)
    expect(sameCluster(a, M(2, { createdAt: '2026-10-09T08:06:00' }))).toBe(false)
    expect(sameCluster(a, M(2, { senderUserId: 9, createdAt: '2026-10-09T08:01:00' }))).toBe(false)
    expect(sameCluster(undefined, a)).toBe(false)
  })
})

describe('time formatting', () => {
  const now = new Date('2026-10-09T12:00:00Z')

  it('says how long ago, in the reader\'s language', () => {
    expect(relativeTime('2026-10-09T11:55:00Z', 'en', now)).toBe('5 minutes ago')
    expect(relativeTime('2026-10-09T09:00:00Z', 'en', now)).toBe('3 hours ago')
    expect(relativeTime('2026-10-08T12:00:00Z', 'en', now)).toBe('yesterday')
    expect(relativeTime('2026-10-09T11:59:50Z', 'en', now)).toBe('now')
    expect(relativeTime(null, 'en', now)).toBe('')
    expect(relativeTime('nonsense', 'en', now)).toBe('')
  })

  it('shows a clock time for today and a date for older conversations', () => {
    expect(inboxTime('2026-10-09T08:30:00', 'en', new Date('2026-10-09T12:00:00'))).toMatch(/8:30/)
    expect(inboxTime('2026-10-01T08:30:00', 'en', new Date('2026-10-09T12:00:00'))).toMatch(/10\/1\/26|1\/10\/26|2026/)
    expect(inboxTime(null, 'en')).toBe('')
  })

  it('labels dividers Today and Yesterday, then a full date', () => {
    const today = new Date('2026-10-09T12:00:00')
    expect(dayLabel(new Date('2026-10-09T01:00:00'), 'en', today)).toBe('today')
    expect(dayLabel(new Date('2026-10-08T23:00:00'), 'en', today)).toBe('yesterday')
    expect(dayLabel(new Date('2026-09-01T10:00:00'), 'en', today)).toMatch(/September 1, 2026/)
  })

  it('formats a bubble clock time and survives a bad date', () => {
    expect(clockTime('2026-10-09T08:30:00', 'en')).toMatch(/8:30/)
    expect(clockTime('nope', 'en')).toBe('')
  })
})

describe('files', () => {
  it('formats sizes', () => {
    expect(formatBytes(512, 'en')).toBe('512 B')
    expect(formatBytes(2048, 'en')).toBe('2 KB')
    expect(formatBytes(5 * 1024 * 1024, 'en')).toBe('5 MB')
  })

  it('names the file kind from the extension, then the content type', () => {
    expect(fileKind('cv.docx', null)).toBe('DOCX')
    expect(fileKind('noext', 'application/pdf')).toBe('PDF')
    expect(fileKind(null, null)).toBe('FILE')
  })
})

describe('filterInbox', () => {
  const items = [C(1, { peer: { userId: 2, name: 'Jane Smith', avatarUrl: null, online: false, lastSeenAt: null }, lastMessagePreview: 'visa documents' }), C(2, { applicationId: 1001, peer: { userId: 3, name: 'Omar', avatarUrl: null, online: false, lastSeenAt: null } })]

  it('matches the person, the preview and the application id, ignoring case', () => {
    expect(filterInbox(items, 'jane').map(c => c.id)).toEqual([1])
    expect(filterInbox(items, 'VISA').map(c => c.id)).toEqual([1])
    expect(filterInbox(items, '1001').map(c => c.id)).toEqual([2])
    expect(filterInbox(items, '  ')).toHaveLength(2)
  })
})
