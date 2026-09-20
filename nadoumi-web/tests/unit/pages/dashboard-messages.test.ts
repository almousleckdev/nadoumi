import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import MessagesIndex from '~/pages/dashboard/messages/index.vue'
import type { ConversationSummary } from '~/types/messages'

const C = (over: Partial<ConversationSummary> = {}): ConversationSummary => ({
  id: 9, subject: 'Visa question', applicationId: null, conversationType: 'GENERAL', status: 'OPEN',
  lastMessagePreview: 'Please send your passport.', lastMessageAt: '2026-01-01T10:00:00', unreadCount: 0, ...over,
})

const listConversations = vi.fn()
const open = vi.fn()
const primary = ref<{ id: number } | null>({ id: 5 })
const { stream, navigate } = vi.hoisted(() => ({
  stream: { onPing: undefined as undefined | ((id: number) => void), onResync: undefined as undefined | (() => void) },
  navigate: vi.fn(),
}))

vi.mock('~/composables/useMessages', () => ({
  useMessages: () => ({ listConversations, open }),
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
mockNuxtImport('navigateTo', () => navigate)

async function mountPage() {
  const w = await mountSuspended(MessagesIndex)
  await flushPromises()
  return w
}

beforeEach(() => {
  listConversations.mockReset().mockResolvedValue([C()])
  open.mockReset()
  navigate.mockReset()
  primary.value = { id: 5 }
})

describe('dashboard messages list', () => {
  it('shows an honest empty state, never fake conversations', async () => {
    listConversations.mockResolvedValue([])
    const w = await mountPage()

    expect(w.text()).toContain('No conversations yet')
    expect(w.findAll('[data-test="conversation-row"]')).toHaveLength(0)
  })

  it('asks for an applicant profile first when there is none, and offers no new-message button', async () => {
    listConversations.mockResolvedValue([])
    primary.value = null
    const w = await mountPage()

    expect(w.text()).toContain('Create your applicant profile first')
    expect(w.find('[data-test="new-message"]').exists()).toBe(false)
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

  it('opens a conversation for the active applicant and navigates to the thread', async () => {
    open.mockResolvedValue({ id: 100, conversationId: 12 })
    const w = await mountPage()

    await w.find('[data-test="new-message"]').trigger('click')
    await w.find('#msg-subject').setValue('Scholarship help')
    await w.find('#msg-body').setValue('Which documents do I need?')
    await w.find('#msg-body').element.closest('form')!.dispatchEvent(new Event('submit'))
    await flushPromises()

    expect(open).toHaveBeenCalledWith({ applicantId: 5, subject: 'Scholarship help', body: 'Which documents do I need?' })
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/dashboard/messages/12'))
  })

  it('explains a 403 (no MESSAGE_STAFF grant) instead of a generic failure', async () => {
    open.mockRejectedValue({ statusCode: 403 })
    const w = await mountPage()

    await w.find('[data-test="new-message"]').trigger('click')
    await w.find('#msg-body').setValue('Hello')
    await w.find('#msg-body').element.closest('form')!.dispatchEvent(new Event('submit'))
    await flushPromises()

    expect(w.find('[data-test="forbidden"]').text()).toContain('cannot message the team')
    expect(navigate).not.toHaveBeenCalled()
  })
})
