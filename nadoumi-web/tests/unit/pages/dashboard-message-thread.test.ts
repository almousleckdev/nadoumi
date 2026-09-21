import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Thread from '~/pages/dashboard/messages/[id].vue'
import { MESSAGE_PAGE_SIZE, type ConversationMessage, type ConversationSummary } from '~/types/messages'

const M = (id: number, over: Partial<ConversationMessage> = {}): ConversationMessage => ({
  id, conversationId: 9, senderUserId: 2, senderName: 'Staff Sam', body: `message ${id}`,
  createdAt: '2026-01-01T10:00:00', editedAt: null, attachments: [], ...over,
})
const SUMMARY: ConversationSummary = {
  id: 9, subject: 'Visa question', applicationId: null, conversationType: 'GENERAL', status: 'OPEN',
  lastMessagePreview: 'x', lastMessageAt: '2026-01-01T10:00:00', unreadCount: 1,
}

const listConversations = vi.fn()
const listMessages = vi.fn()
const post = vi.fn()
const markRead = vi.fn()
const stream = vi.hoisted(() => ({ onPing: undefined as undefined | ((id: number) => void) }))

vi.mock('~/composables/useMessages', () => ({
  useMessages: () => ({ listConversations, listMessages, post, markRead }),
}))
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ user: ref({ userId: 1, username: 'ada', nickName: 'Ada' }) }),
}))
vi.mock('~/composables/useConversationStream', () => ({
  useConversationStream: (onPing: (id: number) => void) => {
    stream.onPing = onPing
    return { reconnecting: ref(false), connected: ref(true) }
  },
}))

mockNuxtImport('useRoute', () => () => ({ params: { id: '9' } }))

async function mountThread() {
  const w = await mountSuspended(Thread)
  await flushPromises()
  return w
}

beforeEach(() => {
  listConversations.mockReset().mockResolvedValue([SUMMARY])
  listMessages.mockReset().mockResolvedValue([M(2), M(1)])
  post.mockReset()
  markRead.mockReset().mockResolvedValue(undefined)
})

describe('dashboard message thread', () => {
  it('renders the thread oldest first with the sender name, and marks it read', async () => {
    const w = await mountThread()

    const texts = w.findAll('[data-test="message"]').map(m => m.text())
    expect(texts[0]).toContain('message 1')
    expect(texts[1]).toContain('message 2')
    expect(texts[0]).toContain('Staff Sam')
    expect(w.text()).toContain('Visa question')
    expect(markRead).toHaveBeenCalledWith(9)
  })

  it('labels the caller\'s own messages "You" rather than their account name', async () => {
    listMessages.mockResolvedValue([M(1, { senderUserId: 1, senderName: 'Ada L.' })])
    const w = await mountThread()

    expect(w.find('[data-test="message"]').text()).toContain('You')
    expect(w.find('[data-test="message"]').text()).not.toContain('Ada L.')
  })

  it('shows an error with retry when the thread cannot be loaded', async () => {
    listMessages.mockReset().mockRejectedValue({ statusCode: 403 })
    const w = await mountThread()
    expect(w.text()).toContain('This section could not be loaded')

    listMessages.mockResolvedValue([M(1)])
    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('message 1')
  })

  it('treats a conversation the caller is not part of as not loadable', async () => {
    listConversations.mockResolvedValue([])
    const w = await mountThread()
    expect(w.text()).toContain('This section could not be loaded')
  })

  it('sends a message, appends the server copy and clears the composer', async () => {
    post.mockResolvedValue(M(3, { senderUserId: 1, senderName: 'Ada', body: 'Thanks!' }))
    const w = await mountThread()

    await w.find('#thread-composer').setValue('Thanks!')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(post).toHaveBeenCalledWith(9, 'Thanks!')
    expect(w.text()).toContain('Thanks!')
    expect((w.find('#thread-composer').element as HTMLTextAreaElement).value).toBe('')
  })

  it('does not send a whitespace-only message', async () => {
    const w = await mountThread()

    await w.find('#thread-composer').setValue('   ')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(post).not.toHaveBeenCalled()
  })

  it('keeps the draft and shows an error when sending fails', async () => {
    post.mockRejectedValue({ statusCode: 500 })
    const w = await mountThread()

    await w.find('#thread-composer').setValue('Hello')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect((w.find('#thread-composer').element as HTMLTextAreaElement).value).toBe('Hello')
    expect(w.text()).toContain('Something went wrong')
  })

  it('pulls in a new message on a live ping for this conversation only', async () => {
    const w = await mountThread()
    listMessages.mockResolvedValue([M(3, { body: 'live reply' }), M(2), M(1)])

    stream.onPing!(77)
    await flushPromises()
    expect(w.text()).not.toContain('live reply')

    stream.onPing!(9)
    await flushPromises()
    expect(w.text()).toContain('live reply')
  })

  it('pages older messages backwards using the oldest id as the cursor', async () => {
    const newest = Array.from({ length: MESSAGE_PAGE_SIZE }, (_, i) => M(100 - i))
    listMessages.mockResolvedValueOnce(newest)
    const w = await mountThread()
    expect(w.find('[data-test="load-older"]').exists()).toBe(true)

    listMessages.mockResolvedValueOnce([M(50, { body: 'ancient' })])
    await w.find('[data-test="load-older"]').trigger('click')
    await flushPromises()

    expect(listMessages).toHaveBeenLastCalledWith(9, 51)
    expect(w.text()).toContain('ancient')
    expect(w.find('[data-test="load-older"]').exists()).toBe(false)
  })

  it('hides the composer on a closed conversation and says so', async () => {
    listConversations.mockResolvedValue([{ ...SUMMARY, status: 'CLOSED' }])
    const w = await mountThread()

    expect(w.find('[data-test="closed"]').exists()).toBe(true)
    expect(w.find('#thread-composer').exists()).toBe(false)
  })
})
