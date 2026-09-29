import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import MessagesIndex from '~/pages/dashboard/messages/index.vue'
import type { ConversationSummary } from '~/types/messages'

const C = (over: Partial<ConversationSummary> = {}): ConversationSummary => ({
  id: 9, subject: 'Visa question', applicationId: null, conversationType: 'GENERAL', status: 'OPEN',
  lastMessagePreview: 'Please send your passport.', lastMessageAt: '2026-01-01T10:00:00', unreadCount: 0, ...over,
})

const listConversations = vi.fn()
const listMessages = vi.fn()
const markRead = vi.fn()
const primary = ref<{ id: number } | null>({ id: 5 })
const { stream } = vi.hoisted(() => ({
  stream: { onPing: undefined as undefined | ((id: number) => void), onResync: undefined as undefined | (() => void) },
}))

vi.mock('~/composables/useMessages', () => ({
  useMessages: () => ({ listConversations, listMessages, markRead }),
}))
vi.mock('~/composables/useMyApplicant', () => ({
  useMyApplicant: () => ({ primary }),
}))
vi.mock('~/composables/useConversationStream', () => ({
  useConversationStream: (onPing: (id: number) => void, onResync: () => void) => {
    stream.onPing = onPing
    stream.onResync = onResync
    return { reconnecting: ref(false), connected: ref(true) }
  },
}))

async function mountPage() {
  const w = await mountSuspended(MessagesIndex)
  await flushPromises()
  return w
}

beforeEach(() => {
  listMessages.mockReset().mockResolvedValue([])
  markRead.mockReset().mockResolvedValue(undefined)
  listConversations.mockReset().mockResolvedValue([C()])
  primary.value = { id: 5 }
})

describe('dashboard messages list', () => {
  it('shows an honest empty state, never fake conversations', async () => {
    listConversations.mockResolvedValue([])
    const w = await mountPage()

    expect(w.text()).toContain('No conversations yet')
    expect(w.findAll('[data-test="conversation-row"]')).toHaveLength(0)
  })

  it('asks for an applicant profile first when there is none', async () => {
    listConversations.mockResolvedValue([])
    primary.value = null
    const w = await mountPage()

    expect(w.text()).toContain('Create your applicant profile first')
  })

  it('allows the student to start a new conversation with a Nadoumi admin', async () => {
    const w = await mountPage()
    expect(w.find('[data-test="new-message"]').exists()).toBe(true)
  })

  it('lists real conversations with preview and unread badge', async () => {
    listConversations.mockResolvedValue([C({ unreadCount: 2 }), C({ id: 10, subject: null, status: 'CLOSED', unreadCount: 0 })])
    const w = await mountPage()

    const rows = w.findAll('[data-test="conversation-row"]')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('Visa question')
    expect(rows[0]!.text()).toContain('Please send your passport.')
    expect(rows[0]!.text()).toContain('2 unread')
    expect(rows[1]!.text()).toContain('Conversation')
    expect(rows[1]!.text()).toContain('Closed')
  })

  it('shows an error with retry when loading fails', async () => {
    listConversations.mockReset().mockRejectedValue(new Error('boom'))
    const w = await mountPage()
    expect(w.text()).toContain('This section could not be loaded')

    listConversations.mockResolvedValueOnce([C()])
    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()

    expect(w.text()).toContain('Visa question')
  })

  it('refreshes silently on a live ping, without flashing the loading state', async () => {
    const w = await mountPage()
    listConversations.mockResolvedValue([C({ unreadCount: 1, lastMessagePreview: 'Fresh reply' })])

    stream.onPing!(9)
    await flushPromises()

    expect(w.text()).toContain('Fresh reply')
    expect(w.text()).toContain('1 unread')
  })

  it('opens a thread oldest first and marks the conversation read', async () => {
    listConversations.mockResolvedValue([C({ unreadCount: 3 })])
    listMessages.mockResolvedValue([
      { id: 12, senderUserId: 1, senderName: 'Amina', body: 'Second message', createdAt: '2026-01-01T10:05:00', attachments: [] },
      { id: 11, senderUserId: 1, senderName: 'Amina', body: 'First message', createdAt: '2026-01-01T10:00:00', attachments: [] },
    ])
    const w = await mountPage()

    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    const bodies = w.findAll('[data-test="message"]').map(m => m.text())
    expect(bodies[0]).toContain('First message')
    expect(bodies[1]).toContain('Second message')
    expect(listMessages).toHaveBeenCalledWith(9)
    expect(markRead).toHaveBeenCalledWith(9)
  })

  it('shows an error with retry in the thread when its messages fail to load', async () => {
    listMessages.mockRejectedValueOnce(new Error('boom'))
    const w = await mountPage()

    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-test="thread-error"]').exists()).toBe(true)
    expect(w.text()).not.toContain('No messages yet')

    listMessages.mockResolvedValueOnce([
      { id: 11, senderUserId: 1, senderName: 'Amina', body: 'Recovered message', createdAt: '2026-01-01T10:00:00', attachments: [] },
    ])
    await w.find('[data-test="thread-error"] button').trigger('click')
    await flushPromises()

    expect(w.find('[data-test="thread-error"]').exists()).toBe(false)
    expect(w.text()).toContain('Recovered message')
  })
})
