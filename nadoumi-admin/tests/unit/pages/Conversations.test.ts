import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listInbox: vi.fn(),
  listMessages: vi.fn(),
  postMessage: vi.fn(),
  uploadAttachment: vi.fn(),
  markConversationRead: vi.fn(),
  closeConversation: vi.fn(),
  addParticipant: vi.fn(),
  MESSAGE_MAX_LENGTH: 4000,
  MESSAGE_PAGE_SIZE: 50,
}))
vi.mock('@/api/conversation', () => api)

const stream = vi.hoisted(() => ({ onPing: undefined as undefined | ((id: number) => void) }))
vi.mock('@/composables/useStaffStream', async () => {
  const { ref } = await import('vue')
  return {
    useStaffStream: (onPing: (id: number) => void) => {
      stream.onPing = onPing
      return { reconnecting: ref(false), connected: ref(true) }
    },
  }
})

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))
vi.mock('vue-router', async (original) => ({
  ...(await original<typeof import('vue-router')>()),
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}))

import Conversations from '@/views/conversations/index.vue'
import { useUserStore } from '@/stores/user'

const C = (over: Record<string, unknown> = {}) => ({
  id: 9, subject: 'Visa question', applicationId: null, conversationType: 'GENERAL', status: 'OPEN',
  lastMessagePreview: 'Please send your passport.', lastMessageAt: '2026-01-01T10:00:00', unreadCount: 2, ...over,
})
const M = (id: number, over: Record<string, unknown> = {}) => ({
  id, conversationId: 9, senderUserId: 5, senderName: 'Amina', body: `message ${id}`,
  createdAt: '2026-01-01T10:00:00', editedAt: null, attachments: [], ...over,
})

async function mountView() {
  const w = mount(Conversations as never, mountOpts())
  await flushPromises()
  return w
}

beforeEach(() => {
  setActivePinia(createPinia())
  const store = useUserStore()
  store.permissions = ['nad:conversation:participate', 'nad:conversation:participant:manage']
  store.userId = 1
  Object.values(api).forEach(fn => typeof fn === 'function' && 'mockReset' in fn && (fn as ReturnType<typeof vi.fn>).mockReset())
  api.listInbox.mockResolvedValue([C()])
  api.listMessages.mockResolvedValue([M(2), M(1)])
  api.markConversationRead.mockResolvedValue(undefined)
  confirm.mockReset()
})

describe('staff conversations', () => {
  it('shows an honest empty state, never fake conversations', async () => {
    api.listInbox.mockResolvedValue([])
    const w = await mountView()
    expect(w.text()).toContain('No conversations yet')
    expect(w.findAll('[data-test="conversation-row"]')).toHaveLength(0)
  })

  it('shows an error with retry when the inbox fails', async () => {
    api.listInbox.mockRejectedValue(new Error('boom'))
    const w = await mountView()
    expect(w.text()).toContain('could not be loaded')
  })

  it('lists inbox rows with preview and unread count', async () => {
    const w = await mountView()
    const row = w.find('[data-test="conversation-row"]')
    expect(row.text()).toContain('Visa question')
    expect(row.text()).toContain('Please send your passport.')
    expect(row.text()).toContain('2')
  })

  it('opens a thread oldest first, marks it read and offers the composer', async () => {
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    const texts = w.findAll('[data-test="message"]').map(m => m.text())
    expect(texts[0]).toContain('message 1')
    expect(texts[1]).toContain('message 2')
    expect(api.markConversationRead).toHaveBeenCalledWith(9)
    expect(w.find('[data-test="composer"]').exists()).toBe(true)
  })

  it('renders attachments with download link when url is present', async () => {
    api.listMessages.mockResolvedValueOnce([
      M(1, {
        attachments: [
          { id: 101, filename: 'transcript.pdf', contentType: 'application/pdf', byteSize: 2048, url: 'https://cdn.example.com/transcript.pdf' },
          { id: 102, filename: 'notes.txt', contentType: 'text/plain', byteSize: 512, url: null },
        ],
      }),
    ])
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    const link = w.find('a.conv__attach-link')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('https://cdn.example.com/transcript.pdf')
    expect(link.text()).toContain('transcript.pdf')

    expect(w.text()).toContain('notes.txt')
  })

  it('offers to join an unclaimed conversation when the thread is forbidden, and joins as STAFF', async () => {
    api.listMessages.mockRejectedValueOnce({ response: { status: 403 } })
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    expect(w.find('[data-test="join-panel"]').exists()).toBe(true)
    expect(w.find('[data-test="composer"]').exists()).toBe(false)

    api.addParticipant.mockResolvedValue(undefined)
    await w.find('[data-test="join"]').trigger('click')
    await flushPromises()

    expect(api.addParticipant).toHaveBeenCalledWith(9, { userId: 1, role: 'STAFF' })
    expect(w.find('[data-test="thread"]').exists()).toBe(true)
  })

  it('does not offer a join button without the manage permission', async () => {
    useUserStore().permissions = ['nad:conversation:participate']
    api.listMessages.mockRejectedValueOnce({ response: { status: 403 } })
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    expect(w.find('[data-test="join"]').exists()).toBe(false)
    expect(w.find('[data-test="join-denied"]').exists()).toBe(true)
  })

  it('sends a reply, appends the server copy and clears the composer', async () => {
    api.postMessage.mockResolvedValue(M(3, { senderUserId: 1, senderName: 'Me', body: 'On it' }))
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    await w.find('textarea').setValue('On it')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.postMessage).toHaveBeenCalledWith(9, 'On it', [])
    expect(w.text()).toContain('On it')
  })

  it('uploads an attachment and sends a reply with attachmentMediaIds', async () => {
    api.uploadAttachment.mockResolvedValueOnce({ mediaId: 55 })
    api.postMessage.mockResolvedValueOnce(
      M(3, {
        senderUserId: 1,
        senderName: 'Me',
        body: 'Here is your doc',
        attachments: [{ id: 1, mediaAssetId: 55, filename: 'guide.pdf', contentType: 'application/pdf', byteSize: 1024, url: 'https://cdn.example.com/guide.pdf' }],
      })
    )
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    const file = new File(['dummy content'], 'guide.pdf', { type: 'application/pdf' })
    const fileInput = w.find('input[data-test="file-input"]')

    Object.defineProperty(fileInput.element, 'files', {
      value: [file],
      writable: true,
    })
    await fileInput.trigger('change')
    await flushPromises()

    expect(api.uploadAttachment).toHaveBeenCalledWith(9, file)
    expect(w.find('[data-test="pending-attachments"]').exists()).toBe(true)
    expect(w.find('[data-test="pending-attachment"]').text()).toContain('guide.pdf')

    await w.find('textarea').setValue('Here is your doc')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.postMessage).toHaveBeenCalledWith(9, 'Here is your doc', [55])
    expect(w.find('[data-test="pending-attachments"]').exists()).toBe(false)
  })

  it('allows sending an attachment without text body', async () => {
    api.uploadAttachment.mockResolvedValueOnce({ mediaId: 56 })
    api.postMessage.mockResolvedValueOnce(M(3, { body: '' }))
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    const file = new File(['photo'], 'passport.png', { type: 'image/png' })
    const fileInput = w.find('input[data-test="file-input"]')
    Object.defineProperty(fileInput.element, 'files', {
      value: [file],
      writable: true,
    })
    await fileInput.trigger('change')
    await flushPromises()

    const sendBtn = w.find('button[data-test="send"]')
    expect(sendBtn.attributes('disabled')).toBeUndefined()

    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.postMessage).toHaveBeenCalledWith(9, '', [56])
  })

  it('removes pending attachment when remove button is clicked', async () => {
    api.uploadAttachment.mockResolvedValueOnce({ mediaId: 57 })
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    const file = new File(['text'], 'notes.pdf', { type: 'application/pdf' })
    const fileInput = w.find('input[data-test="file-input"]')
    Object.defineProperty(fileInput.element, 'files', {
      value: [file],
      writable: true,
    })
    await fileInput.trigger('change')
    await flushPromises()

    expect(w.find('[data-test="pending-attachment"]').exists()).toBe(true)
    await w.find('[data-test="remove-pending"]').trigger('click')

    expect(w.find('[data-test="pending-attachments"]').exists()).toBe(false)
  })

  it('pulls in a new message on a live ping for the open conversation only', async () => {
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()
    api.listMessages.mockResolvedValue([M(3, { body: 'live reply' }), M(2), M(1)])

    stream.onPing!(77)
    await flushPromises()
    expect(w.text()).not.toContain('live reply')

    stream.onPing!(9)
    await flushPromises()
    expect(w.text()).toContain('live reply')
  })

  it('closes a conversation only after confirmation', async () => {
    confirm.mockResolvedValue(false)
    const w = await mountView()
    await w.find('[data-test="conversation-row"]').trigger('click')
    await flushPromises()

    await w.find('[data-test="close"]').trigger('click')
    await flushPromises()
    expect(api.closeConversation).not.toHaveBeenCalled()

    confirm.mockResolvedValue(true)
    api.closeConversation.mockResolvedValue(undefined)
    await w.find('[data-test="close"]').trigger('click')
    await flushPromises()
    expect(api.closeConversation).toHaveBeenCalledWith(9)
  })
})
