import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'
import type { ChatConversation, ChatMessage, ChatPerson, StudentResult } from '@/api/conversation'

const ME = 1

const api = vi.hoisted(() => ({
  listInbox: vi.fn(), searchStudents: vi.fn(), openDirect: vi.fn(), listMessages: vi.fn(), postMessage: vi.fn(),
  uploadAttachment: vi.fn(), markConversationRead: vi.fn(), closeConversation: vi.fn(), deleteConversation: vi.fn(),
}))
vi.mock('@/api/conversation', async (original) => ({ ...(await original<typeof import('@/api/conversation')>()), ...api }))

const live = vi.hoisted(() => ({ handlers: null as null | Record<string, (...a: never[]) => void> }))
vi.mock('@/composables/useStaffChatStream', async () => {
  const { ref } = await import('vue')
  return {
    useStaffChatStream: (handlers: Record<string, (...a: never[]) => void>) => {
      live.handlers = handlers
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
import { SEARCH_DEBOUNCE_MS } from '@/composables/useStudentSearch'

const person = (over: Partial<ChatPerson> = {}): ChatPerson => ({
  userId: 100, name: 'Amina', avatarUrl: null, online: false, lastSeenAt: null, ...over,
})
const conv = (id: number, over: Partial<ChatConversation> = {}): ChatConversation => ({
  id, subject: null, applicationId: null, conversationType: 'DIRECT', status: 'OPEN', lastMessageId: 10,
  lastMessagePreview: 'Hello advisor', lastMessageAt: '2026-10-09T08:00:00', lastSenderUserId: 100, unreadCount: 2,
  peer: person(), peerDeliveredMessageId: null, peerReadMessageId: null, ...over,
})
const msg = (id: number, over: Partial<ChatMessage> = {}): ChatMessage => ({
  id, conversationId: 1, senderUserId: 100, senderName: 'Amina', body: `message ${id}`, createdAt: '2026-10-09T08:00:00',
  editedAt: null, attachments: [], ...over,
})
const student = (id: number, name: string, over: Partial<StudentResult> = {}): StudentResult => ({
  userId: id, studentRef: `STU-${id}`, name, avatarUrl: null, online: false, matchedApplicationId: null, ...over,
})

const mounted: Array<{ unmount: () => void }> = []
async function mountPage() {
  const w = mount(Conversations, mountOpts())
  mounted.push(w)
  await flushPromises()
  return w
}
async function openFirst(w: Awaited<ReturnType<typeof mountPage>>) {
  await w.find('[data-test="inbox-item"]').trigger('click')
  await flushPromises()
}
function emit(name: string, payload: unknown) {
  (live.handlers![name] as (p: unknown) => void)(payload)
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval'] })
  setActivePinia(createPinia())
  useUserStore().userId = ME
  Object.values(api).forEach(fn => fn.mockReset())
  confirm.mockReset().mockResolvedValue(true)
  api.listInbox.mockResolvedValue([conv(1), conv(2, { peer: person({ userId: 101, name: 'Bilal' }), unreadCount: 0, lastMessagePreview: 'Thanks', lastMessageAt: '2026-10-08T08:00:00' })])
  api.listMessages.mockResolvedValue([msg(10), msg(9)])
  api.markConversationRead.mockResolvedValue(undefined)
  api.searchStudents.mockResolvedValue([])
})
afterEach(() => {
  mounted.splice(0).forEach(w => w.unmount()) // teleported overlays must not leak into the next test
  vi.useRealTimers()
})

describe('inbox', () => {
  it('lists the staff member\'s own conversations with the student\'s first name, preview and unread count', async () => {
    const w = await mountPage()
    const rows = w.findAll('[data-test="inbox-item"]')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('Amina')
    expect(rows[0]!.text()).toContain('Hello advisor')
    expect(rows[0]!.find('[data-test="unread"]').text()).toBe('2')
    expect(api.listInbox).toHaveBeenCalledWith({ page: 0 }, true)
  })

  it('shows an honest empty state, never fake chats', async () => {
    api.listInbox.mockResolvedValue([])
    const w = await mountPage()
    expect(w.find('[data-test="inbox-empty"]').exists()).toBe(true)
  })

  it('shows an error with a retry', async () => {
    api.listInbox.mockRejectedValueOnce(new Error('down'))
    const w = await mountPage()
    expect(w.find('[data-test="inbox-error"]').exists()).toBe(true)
    await w.find('[data-test="inbox-error"] button').trigger('click')
    await flushPromises()
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(2)
  })
})

describe('finding students', () => {
  async function type(w: Awaited<ReturnType<typeof mountPage>>, text: string) {
    await w.find('input[data-test="inbox-search"]').setValue(text)
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    await flushPromises()
  }

  it('searches the server (not a loaded list) and narrows the inbox by name at the same time', async () => {
    api.searchStudents.mockResolvedValue([student(300, 'Amira', { matchedApplicationId: 1001 })])
    const w = await mountPage()
    await type(w, 'ami')
    expect(api.searchStudents).toHaveBeenCalledWith('ami')
    expect(api.listInbox).toHaveBeenLastCalledWith({ q: 'ami', page: 0 }, true)
    const option = w.find('[data-test="student-option"]')
    expect(option.text()).toContain('Amira')
    expect(option.text()).toContain('STU-300')
    expect(option.text()).toContain('Application #1001')
  })

  it('treats an application reference as an application filter on the inbox', async () => {
    const w = await mountPage()
    await type(w, 'APP-1001')
    expect(api.searchStudents).toHaveBeenCalledWith('APP-1001')
    expect(api.listInbox).toHaveBeenLastCalledWith({ applicationId: 1001, page: 0 }, true)
  })

  it('asks nothing for a single character', async () => {
    const w = await mountPage()
    await type(w, 'a')
    expect(api.searchStudents).not.toHaveBeenCalled()
  })

  it('says so when no student matches', async () => {
    const w = await mountPage()
    await type(w, 'zzz')
    expect(w.find('[data-test="no-students"]').exists()).toBe(true)
  })

  it('opens (or creates) the private chat with the chosen student and shows the thread', async () => {
    api.searchStudents.mockResolvedValue([student(300, 'Amira')])
    api.openDirect.mockResolvedValue(conv(7, { peer: person({ userId: 300, name: 'Amira' }), unreadCount: 0, lastMessageId: null, lastMessagePreview: null }))
    api.listMessages.mockResolvedValue([])
    const w = await mountPage()
    await type(w, 'ami')
    await w.find('[data-test="student-option"]').trigger('click')
    await flushPromises()
    expect(api.openDirect).toHaveBeenCalledWith(300)
    expect(w.find('[data-test="peer-name"]').text()).toBe('Amira')
    expect(w.find('[data-test="thread-empty"]').exists()).toBe(true)
  })
})

describe('the open conversation', () => {
  it('loads one page, marks it read once and does not re-fetch the inbox', async () => {
    const w = await mountPage()
    await openFirst(w)
    expect(api.listMessages).toHaveBeenCalledWith(1, 0, true)
    expect(api.markConversationRead).toHaveBeenCalledWith(1)
    expect(api.listInbox).toHaveBeenCalledTimes(1)
    expect(w.findAll('[data-message-id]').map(e => e.attributes('data-message-id'))).toEqual(['9', '10'])
  })

  it('shows the student\'s presence in the header', async () => {
    api.listInbox.mockResolvedValue([conv(1, { peer: person({ online: true }) })])
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="presence"]').text()).toBe('Online')
  })

  it('closes the chat after a confirmation, and not without one', async () => {
    api.closeConversation.mockResolvedValue(undefined)
    const w = await mountPage()
    await openFirst(w)
    confirm.mockResolvedValueOnce(false)
    await w.find('[data-test="close-chat"]').trigger('click')
    await flushPromises()
    expect(api.closeConversation).not.toHaveBeenCalled()
    await w.find('[data-test="close-chat"]').trigger('click')
    await flushPromises()
    expect(api.closeConversation).toHaveBeenCalledWith(1)
    expect(w.find('[data-test="closed"]').exists()).toBe(true)
    expect(w.find('[data-test="composer"]').exists()).toBe(false)
  })

  it('offers to reopen a closed chat', async () => {
    api.listInbox.mockResolvedValue([conv(1, { status: 'CLOSED' })])
    api.openDirect.mockResolvedValue(conv(1, { status: 'OPEN' }))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="closed"] button').trigger('click')
    await flushPromises()
    expect(api.openDirect).toHaveBeenCalledWith(100)
    expect(w.find('[data-test="composer"]').exists()).toBe(true)
  })

  it('loads earlier messages with the oldest id as the cursor', async () => {
    api.listMessages.mockResolvedValueOnce(Array.from({ length: 30 }, (_, i) => msg(100 - i)))
    api.listMessages.mockResolvedValueOnce([msg(69), msg(68)])
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="load-older"]').trigger('click')
    await flushPromises()
    expect(api.listMessages).toHaveBeenLastCalledWith(1, 71, true)
    expect(w.find('[data-message-id="68"]').exists()).toBe(true)
  })
})

describe('deleting a chat', () => {
  it('offers no delete for a support ticket chat, which is deleted from Support', async () => {
    api.listInbox.mockResolvedValue([conv(1, { conversationType: 'SUPPORT' })])
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="peer-name"]').exists()).toBe(true)
    expect(w.find('[data-test="delete-chat"]').exists()).toBe(false)
  })

  it('goes back to the list with the back button', async () => {
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="back"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-test="peer-name"]').exists()).toBe(false)
  })

  it('deletes the chat for good after a confirmation, and not without one', async () => {
    api.deleteConversation.mockResolvedValue(undefined)
    const w = await mountPage()
    await openFirst(w)
    confirm.mockResolvedValueOnce(false)
    await w.find('[data-test="delete-chat"]').trigger('click')
    await flushPromises()
    expect(api.deleteConversation).not.toHaveBeenCalled()
    await w.find('[data-test="delete-chat"]').trigger('click')
    await flushPromises()
    expect(api.deleteConversation).toHaveBeenCalledWith(1)
    expect(confirm).toHaveBeenLastCalledWith(expect.objectContaining({ tone: 'danger' }))
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(1)
    expect(w.find('[data-test="peer-name"]').exists()).toBe(false)
  })

  it('keeps the chat and reports it when the delete fails', async () => {
    api.deleteConversation.mockRejectedValue(new Error('forbidden'))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="delete-chat"]').trigger('click')
    await flushPromises()
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(2)
  })

  it('drops a chat a colleague deleted, live', async () => {
    const w = await mountPage()
    await openFirst(w)
    emit('removed', { conversationId: 1 })
    await flushPromises()
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(1)
    expect(w.find('[data-test="peer-name"]').exists()).toBe(false)
  })
})

describe('sending an image', () => {
  it('shows the picture itself while it is being sent, not a document card', async () => {
    let resolve!: (m: ChatMessage) => void
    api.uploadAttachment.mockResolvedValue({ mediaId: 77 })
    api.postMessage.mockReturnValue(new Promise<ChatMessage>((r) => { resolve = r }))
    URL.createObjectURL = () => 'blob:preview-1'
    URL.revokeObjectURL = vi.fn()
    const w = await mountPage()
    await openFirst(w)
    const input = w.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [new File(['x'], 'pic.png', { type: 'image/png' })], configurable: true })
    await input.trigger('change')
    await flushPromises()
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[data-test="attachment-sending-image"] img').attributes('src')).toBe('blob:preview-1')
    expect(w.find('[data-test="attachment-file"]').exists()).toBe(false)

    resolve(msg(40, { senderUserId: ME, body: '', attachments: [{ id: 9, filename: 'pic.png', contentType: 'image/png', byteSize: 1, image: true }] }))
    await flushPromises()
    expect(w.find('[data-test="attachment-sending-image"]').exists()).toBe(false)
    expect(w.find('[data-test="attachment-image"] img').attributes('src')).toContain('/api/staff/conversations/1/attachments/9')
  })
})

describe('live events', () => {
  it('appends a student\'s message to the open thread instantly, with no re-fetch', async () => {
    const w = await mountPage()
    await openFirst(w)
    emit('message', msg(11, { body: 'Just arrived' }))
    await flushPromises()
    expect(w.text()).toContain('Just arrived')
    expect(api.listMessages).toHaveBeenCalledTimes(1)
    expect(api.listInbox).toHaveBeenCalledTimes(1)
  })

  it('bumps another conversation to the top with its badge', async () => {
    const w = await mountPage()
    emit('message', msg(30, { conversationId: 2, senderUserId: 101, senderName: 'Bilal', body: 'Any news?' }))
    await flushPromises()
    const first = w.findAll('[data-test="inbox-item"]')[0]!
    expect(first.text()).toContain('Bilal')
    expect(first.text()).toContain('Any news?')
    expect(first.find('[data-test="unread"]').text()).toBe('1')
  })

  it('moves a sent tick to delivered and read as receipts arrive', async () => {
    api.listMessages.mockResolvedValue([msg(10, { senderUserId: ME })])
    const w = await mountPage()
    await openFirst(w)
    const status = () => w.find('[data-message-id="10"] [data-status]').attributes('data-status')
    expect(status()).toBe('sent')
    emit('delivered', { conversationId: 1, userId: 100, messageId: 10 })
    await flushPromises()
    expect(status()).toBe('delivered')
    emit('read', { conversationId: 1, userId: 100, messageId: 10 })
    await flushPromises()
    expect(status()).toBe('read')
  })

  it('flips the presence line when the student comes online', async () => {
    const w = await mountPage()
    await openFirst(w)
    emit('presence', { userId: 100, online: true, lastSeenAt: null })
    await flushPromises()
    expect(w.find('[data-test="presence"]').text()).toBe('Online')
  })
})

describe('sending', () => {
  it('shows the reply at once and confirms it when the server answers', async () => {
    let resolve!: (m: ChatMessage) => void
    api.postMessage.mockReturnValue(new Promise<ChatMessage>((r) => { resolve = r }))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="composer"]').setValue('We will review it today')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('We will review it today')
    expect(w.find('[data-status="sending"]').exists()).toBe(true)
    resolve(msg(12, { senderUserId: ME, body: 'We will review it today' }))
    await flushPromises()
    expect(w.find('[data-status="sending"]').exists()).toBe(false)
    expect(api.postMessage).toHaveBeenCalledWith(1, 'We will review it today', [])
  })

  it('keeps a failed reply with Retry and re-sends it', async () => {
    api.postMessage.mockRejectedValueOnce(new Error('offline'))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="composer"]').setValue('Please resend')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[data-test="retry"]').exists()).toBe(true)
    api.postMessage.mockResolvedValueOnce(msg(13, { senderUserId: ME, body: 'Please resend' }))
    await w.find('[data-test="retry"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-test="retry"]').exists()).toBe(false)
    expect(api.postMessage).toHaveBeenCalledTimes(2)
  })

  it('sends on Enter and adds a line on Shift+Enter', async () => {
    api.postMessage.mockResolvedValue(msg(14, { senderUserId: ME }))
    const w = await mountPage()
    await openFirst(w)
    const box = w.find('[data-test="composer"]')
    await box.setValue('first line')
    await box.trigger('keydown', { key: 'Enter', shiftKey: true })
    expect(api.postMessage).not.toHaveBeenCalled()
    await box.trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(api.postMessage).toHaveBeenCalledTimes(1)
  })

  it('inserts an emoji from the picker', async () => {
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="emoji-toggle"]').trigger('click')
    await w.find('[data-test="emoji-picker"] [role="tabpanel"] button').trigger('click')
    expect((w.find('[data-test="composer"]').element as HTMLTextAreaElement).value).toContain('😀')
  })
})

describe('attachments', () => {
  it('uploads a valid file, then sends its media id', async () => {
    api.uploadAttachment.mockResolvedValue({ mediaId: 55 })
    api.postMessage.mockResolvedValue(msg(15, { senderUserId: ME, body: '' }))
    const w = await mountPage()
    await openFirst(w)
    const input = w.find('input[type="file"]')
    const file = new File(['%PDF-1.4'], 'offer.pdf', { type: 'application/pdf' })
    Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
    await input.trigger('change')
    await flushPromises()
    expect(api.uploadAttachment).toHaveBeenCalledWith(1, file)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.postMessage).toHaveBeenCalledWith(1, '', [55])
  })

  it('rejects a file type the server would refuse, without uploading it', async () => {
    const w = await mountPage()
    await openFirst(w)
    const input = w.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [new File(['x'], 'run.exe', { type: 'application/x-msdownload' })], configurable: true })
    await input.trigger('change')
    await flushPromises()
    expect(api.uploadAttachment).not.toHaveBeenCalled()
    expect(w.find('[role="alert"]').text()).toContain('Only images')
  })

  it('previews an image and offers other files as downloads through the checked endpoint', async () => {
    api.listMessages.mockResolvedValue([msg(10, {
      attachments: [
        { id: 5, filename: 'photo.png', contentType: 'image/png', byteSize: 2048, image: true },
        { id: 6, filename: 'cv.docx', contentType: 'application/x-docx', byteSize: 4096, image: false },
      ],
    })])
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="attachment-image"] img').attributes('src')).toContain('/api/staff/conversations/1/attachments/5')
    const file = w.find('[data-test="attachment-file"]')
    expect(file.attributes('href')).toContain('/api/staff/conversations/1/attachments/6?download=1')
    expect(file.text()).toContain('cv.docx')
    await w.find('[data-test="attachment-image"]').trigger('click')
    expect(document.body.querySelector('[data-test="lightbox"]')).not.toBeNull()
  })
})

describe('image viewer', () => {
  const images = (n: number) => Array.from({ length: n }, (_, i) => ({ id: 20 + i, filename: `pic${i + 1}.png`, contentType: 'image/png', byteSize: 1024, image: true }))
  const lightbox = () => document.body.querySelector('[data-test="lightbox"]')
  const click = (sel: string) => (document.body.querySelector(sel) as HTMLElement).click()
  const name = () => document.body.querySelector('[data-test="lightbox-name"]')!.textContent!.trim()

  it('opens the clicked image in a card and steps through every image of the chat, wrapping around', async () => {
    api.listMessages.mockResolvedValue([msg(10, { attachments: images(3) })])
    const w = await mountPage()
    await openFirst(w)
    await w.findAll('[data-test="attachment-image"]')[1]!.trigger('click')
    await flushPromises()
    expect(name()).toBe('pic2.png')
    expect(document.body.querySelector('[data-test="lightbox-counter"]')!.textContent!.trim()).toBe('2 of 3')
    click('[data-test="lightbox-next"]')
    await flushPromises()
    expect(name()).toBe('pic3.png')
    click('[data-test="lightbox-next"]')
    await flushPromises()
    expect(name()).toBe('pic1.png')
    click('[data-test="lightbox-prev"]')
    await flushPromises()
    expect(name()).toBe('pic3.png')
  })

  it('answers the arrow keys and closes on Escape', async () => {
    api.listMessages.mockResolvedValue([msg(10, { attachments: images(2) })])
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="attachment-image"]').trigger('click')
    await flushPromises()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await flushPromises()
    expect(name()).toBe('pic2.png')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(lightbox()).toBeNull()
  })

  it('shows no arrows or counter for a single image, and closes with the X', async () => {
    api.listMessages.mockResolvedValue([msg(10, { attachments: images(1) })])
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="attachment-image"]').trigger('click')
    await flushPromises()
    expect(document.body.querySelector('[data-test="lightbox-next"]')).toBeNull()
    click('[data-test="lightbox-close"]')
    await flushPromises()
    expect(lightbox()).toBeNull()
  })
})
