import { describe, it, expect, vi, beforeEach } from 'vitest'

const request = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('@/utils/request', () => ({ default: request }))

import {
  addParticipant, closeConversation, listInbox, listMessages, listParticipants, markConversationRead, postMessage,
} from '@/api/conversation'

beforeEach(() => {
  request.get.mockReset().mockResolvedValue([])
  request.post.mockReset().mockResolvedValue(undefined)
})

describe('conversation api', () => {
  it('reads the staff inbox', async () => {
    await listInbox()
    expect(request.get).toHaveBeenCalledWith('/api/staff/conversations')
  })

  it('pages messages with the beforeId cursor and can mark the probe silent', async () => {
    await listMessages(9)
    await listMessages(9, 41, true)
    expect(request.get).toHaveBeenNthCalledWith(1, '/api/staff/conversations/9/messages', { params: { beforeId: 0 }, silent: false })
    expect(request.get).toHaveBeenNthCalledWith(2, '/api/staff/conversations/9/messages', { params: { beforeId: 41 }, silent: true })
  })

  it('posts a reply body only', async () => {
    await postMessage(9, 'hello')
    expect(request.post).toHaveBeenCalledWith('/api/staff/conversations/9/messages', { body: 'hello' })
  })

  it('marks read, lists participants and closes', async () => {
    await markConversationRead(9)
    await listParticipants(9)
    await closeConversation(9)
    expect(request.post).toHaveBeenCalledWith('/api/staff/conversations/9/read')
    expect(request.get).toHaveBeenCalledWith('/api/staff/conversations/9/participants')
    expect(request.post).toHaveBeenCalledWith('/api/staff/conversations/9/close')
  })

  it('adds a participant with an explicit role', async () => {
    await addParticipant(9, { userId: 7, role: 'STAFF' })
    expect(request.post).toHaveBeenCalledWith('/api/staff/conversations/9/participants', { userId: 7, role: 'STAFF' })
  })
})
