import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import { useSession } from '~/composables/useSession'
import Messages from '~/pages/dashboard/messages/index.vue'
import type { ChatConversation, ChatMessage, ChatPerson } from '~/types/chat'

const ME = 7

const api = vi.hoisted(() => ({
  listInbox: vi.fn(), listStaff: vi.fn(), openDirect: vi.fn(), listMessages: vi.fn(),
  post: vi.fn(), markRead: vi.fn(), uploadAttachment: vi.fn(),
}))
const live = vi.hoisted(() => ({ handlers: null as null | Record<string, (...a: never[]) => void> }))

vi.mock('~/composables/useChatApi', () => ({
  useChatApi: () => api,
  attachmentUrl: (c: number, a: number, d = false) => `/api/student-attachment/${c}/${a}${d ? '?download=1' : ''}`,
}))
vi.mock('~/composables/useChatStream', async () => {
  const { ref } = await import('vue')
  return {
    useChatStream: (handlers: Record<string, (...a: never[]) => void>) => {
      live.handlers = handlers
      return { connected: ref(true), reconnecting: ref(false) }
    },
  }
})
vi.mock('~/composables/useChatUnread', async () => {
  const { ref } = await import('vue')
  return { useChatUnread: () => ({ count: ref(0), refresh: vi.fn() }) }
})

const person = (over: Partial<ChatPerson> = {}): ChatPerson => ({
  userId: 2, name: 'Jane', avatarUrl: null, online: false, lastSeenAt: null, ...over,
})
const conv = (id: number, over: Partial<ChatConversation> = {}): ChatConversation => ({
  id, subject: null, applicationId: null, conversationType: 'DIRECT', status: 'OPEN', lastMessageId: 10,
  lastMessagePreview: 'Welcome aboard', lastMessageAt: '2026-10-09T08:00:00', lastSenderUserId: 2, unreadCount: 2,
  peer: person(), peerDeliveredMessageId: null, peerReadMessageId: null, ...over,
})
const msg = (id: number, over: Partial<ChatMessage> = {}): ChatMessage => ({
  id, conversationId: 1, senderUserId: 2, senderName: 'Jane', body: `message ${id}`, createdAt: '2026-10-09T08:00:00',
  editedAt: null, attachments: [], ...over,
})

const mounted: VueWrapper[] = []
async function mountPage(): Promise<VueWrapper> {
  const w = await mountSuspended(Messages)
  mounted.push(w)
  await flushPromises()
  return w
}
afterEach(() => {
  // teleported overlays (viewer, modal) live on document.body and must not leak into the next test
  mounted.splice(0).forEach(w => w.unmount())
})
async function openFirst(w: VueWrapper) {
  await w.find('[data-test="inbox-item"]').trigger('click')
  await flushPromises()
}
function emit(name: string, payload: unknown) {
  ;(live.handlers![name] as (p: unknown) => void)(payload)
}

beforeEach(() => {
  useSession().user.value = { userId: ME } as never
  Object.values(api).forEach(fn => fn.mockReset())
  api.listInbox.mockResolvedValue([conv(1), conv(2, { peer: person({ userId: 3, name: 'Omar' }), unreadCount: 0, lastMessagePreview: 'See you', lastMessageAt: '2026-10-08T08:00:00' })])
  api.listMessages.mockResolvedValue([msg(10), msg(9)])
  api.markRead.mockResolvedValue(undefined)
  api.listStaff.mockResolvedValue([person({ userId: 2, name: 'Jane', online: true }), person({ userId: 3, name: 'Omar' })])
})

describe('inbox', () => {
  it('lists the real conversations with the person\'s name, preview and unread count', async () => {
    const w = await mountPage()
    const rows = w.findAll('[data-test="inbox-item"]')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('Jane')
    expect(rows[0]!.text()).toContain('Welcome aboard')
    expect(rows[0]!.find('[data-test="unread"]').text()).toBe('2')
    expect(api.listInbox).toHaveBeenCalledTimes(1)
  })

  it('shows an honest empty state with a way to start a chat, never fake chats', async () => {
    api.listInbox.mockResolvedValue([])
    const w = await mountPage()
    expect(w.find('[data-test="inbox-empty"]').exists()).toBe(true)
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(0)
  })

  it('shows an error with a retry that loads again', async () => {
    api.listInbox.mockRejectedValueOnce(new Error('down'))
    const w = await mountPage()
    expect(w.find('[data-test="inbox-error"]').exists()).toBe(true)
    await w.find('[data-test="inbox-error"] button').trigger('click')
    await flushPromises()
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(2)
  })

  it('filters locally as you type', async () => {
    const w = await mountPage()
    await w.find('[data-test="inbox-search"]').setValue('omar')
    expect(w.findAll('[data-test="inbox-item"]')).toHaveLength(1)
    await w.find('[data-test="inbox-search"]').setValue('zzz')
    expect(w.find('[data-test="inbox-no-results"]').exists()).toBe(true)
  })
})

describe('opening a conversation', () => {
  it('loads one page of messages, marks it read once, and clears the badge without re-fetching the inbox', async () => {
    const w = await mountPage()
    await openFirst(w)
    expect(api.listMessages).toHaveBeenCalledWith(1)
    expect(api.markRead).toHaveBeenCalledWith(1)
    expect(api.listInbox).toHaveBeenCalledTimes(1)
    expect(w.findAll('[data-message-id]').map(e => e.attributes('data-message-id'))).toEqual(['9', '10'])
    expect(w.find('[data-test="unread"]').exists()).toBe(false)
  })

  it('shows the person\'s presence in the header', async () => {
    api.listInbox.mockResolvedValue([conv(1, { peer: person({ online: true }) })])
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="presence"]').text()).toBe('Online')
  })
})

describe('live events', () => {
  it('appends a message to the open thread instantly and acknowledges it', async () => {
    const w = await mountPage()
    await openFirst(w)
    api.markRead.mockClear()
    emit('message', msg(11, { body: 'Just arrived' }))
    await flushPromises()
    expect(w.text()).toContain('Just arrived')
    expect(api.markRead).toHaveBeenCalledWith(1)
    expect(api.listMessages).toHaveBeenCalledTimes(1) // nothing re-fetched
    expect(api.listInbox).toHaveBeenCalledTimes(1)
  })

  it('bumps the badge and the preview of another conversation and moves it to the top', async () => {
    const w = await mountPage()
    emit('message', msg(30, { conversationId: 2, senderUserId: 3, senderName: 'Omar', body: 'Are you there?' }))
    await flushPromises()
    const rows = w.findAll('[data-test="inbox-item"]')
    expect(rows[0]!.text()).toContain('Omar')
    expect(rows[0]!.text()).toContain('Are you there?')
    expect(rows[0]!.find('[data-test="unread"]').text()).toBe('1')
  })

  it('turns a sent tick into delivered and read as the receipts arrive', async () => {
    api.listMessages.mockResolvedValue([msg(10, { senderUserId: ME })])
    const w = await mountPage()
    await openFirst(w)
    const status = () => w.find('[data-message-id="10"] [data-status]').attributes('data-status')
    expect(status()).toBe('sent')
    emit('delivered', { conversationId: 1, userId: 2, messageId: 10 })
    await flushPromises()
    expect(status()).toBe('delivered')
    emit('read', { conversationId: 1, userId: 2, messageId: 10 })
    await flushPromises()
    expect(status()).toBe('read')
  })

  it('flips the presence line when the other person comes online', async () => {
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="presence"]').text()).not.toBe('Online')
    emit('presence', { userId: 2, online: true, lastSeenAt: null })
    await flushPromises()
    expect(w.find('[data-test="presence"]').text()).toBe('Online')
  })
})

describe('sending', () => {
  it('shows the message at once and confirms it when the server answers', async () => {
    let resolve!: (m: ChatMessage) => void
    api.post.mockReturnValue(new Promise<ChatMessage>((r) => { resolve = r }))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="composer"]').setValue('Hello Jane')
    await w.find('[data-test="send"]').trigger('submit')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Hello Jane')
    expect(w.find('[data-status="sending"]').exists()).toBe(true)

    resolve(msg(12, { senderUserId: ME, body: 'Hello Jane' }))
    await flushPromises()
    expect(w.find('[data-status="sending"]').exists()).toBe(false)
    expect(w.findAll('[data-message-id]').map(e => e.attributes('data-message-id'))).toContain('12')
    expect(api.post).toHaveBeenCalledTimes(1)
    expect(api.post).toHaveBeenCalledWith(1, 'Hello Jane', [])
  })

  it('keeps a failed message with Retry and Delete, and re-sends it on Retry', async () => {
    api.post.mockRejectedValueOnce(new Error('offline'))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="composer"]').setValue('Please help')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[data-test="retry"]').exists()).toBe(true)
    expect(w.text()).toContain('Please help')

    api.post.mockResolvedValueOnce(msg(13, { senderUserId: ME, body: 'Please help' }))
    await w.find('[data-test="retry"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-test="retry"]').exists()).toBe(false)
    expect(api.post).toHaveBeenCalledTimes(2)
  })

  it('removes a failed message on Delete', async () => {
    api.post.mockRejectedValueOnce(new Error('offline'))
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="composer"]').setValue('Oops')
    await w.find('form').trigger('submit')
    await flushPromises()
    await w.find('[data-test="discard"]').trigger('click')
    expect(w.text()).not.toContain('Oops')
  })

  it('sends on Enter and adds a line on Shift+Enter', async () => {
    api.post.mockResolvedValue(msg(14, { senderUserId: ME }))
    const w = await mountPage()
    await openFirst(w)
    const box = w.find('[data-test="composer"]')
    await box.setValue('first line')
    await box.trigger('keydown', { key: 'Enter', shiftKey: true })
    expect(api.post).not.toHaveBeenCalled()
    await box.trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(api.post).toHaveBeenCalledTimes(1)
  })

  it('does not send an empty message', async () => {
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="composer"]').setValue('   ')
    await w.find('form').trigger('submit')
    expect(api.post).not.toHaveBeenCalled()
    expect(w.find('[data-test="send"]').attributes('disabled')).toBeDefined()
  })

  it('inserts an emoji from the picker', async () => {
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="emoji-toggle"]').trigger('click')
    const first = w.find('[data-test="emoji-picker"] [role="tabpanel"] button')
    await first.trigger('click')
    expect((w.find('[data-test="composer"]').element as HTMLTextAreaElement).value).toContain('😀')
  })
})

describe('attachments', () => {
  it('uploads a valid file, then sends its media id with the message', async () => {
    api.uploadAttachment.mockResolvedValue({ mediaId: 55 })
    api.post.mockResolvedValue(msg(15, { senderUserId: ME, body: '' }))
    const w = await mountPage()
    await openFirst(w)
    const input = w.find('input[type="file"]')
    const file = new File(['%PDF-1.4'], 'passport.pdf', { type: 'application/pdf' })
    Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
    await input.trigger('change')
    await flushPromises()
    expect(api.uploadAttachment).toHaveBeenCalledWith(1, file)
    expect(w.find('[data-test="pending-file"]').text()).toContain('passport.pdf')

    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(1, '', [55])
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

  it('previews an image through the same-origin redirect and offers files as downloads', async () => {
    api.listMessages.mockResolvedValue([msg(10, {
      attachments: [
        { id: 5, filename: 'photo.png', contentType: 'image/png', byteSize: 2048, image: true },
        { id: 6, filename: 'cv.docx', contentType: 'application/x-docx', byteSize: 4096, image: false },
      ],
    })])
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="attachment-image"] img').attributes('src')).toBe('/api/student-attachment/1/5')
    const file = w.find('[data-test="attachment-file"]')
    expect(file.attributes('href')).toBe('/api/student-attachment/1/6?download=1')
    expect(file.text()).toContain('cv.docx')
    expect(file.text()).toContain('4 KB')
  })
})

describe('image viewer', () => {
  const images = (n: number) => Array.from({ length: n }, (_, i) => ({ id: 20 + i, filename: `pic${i + 1}.png`, contentType: 'image/png', byteSize: 1024, image: true }))
  const lightbox = () => document.body.querySelector('[data-test="lightbox"]')
  const click = (sel: string) => (document.body.querySelector(sel) as HTMLElement).click()
  const name = () => document.body.querySelector('[data-test="lightbox-name"]')!.textContent

  it('opens the clicked image in a card and steps through every image of the chat', async () => {
    api.listMessages.mockResolvedValue([msg(10, { attachments: images(3) })])
    const w = await mountPage()
    await openFirst(w)
    await w.findAll('[data-test="attachment-image"]')[1]!.trigger('click')
    await flushPromises()
    expect(lightbox()).not.toBeNull()
    expect(name()).toBe('pic2.png')
    expect(document.body.querySelector('[data-test="lightbox-counter"]')!.textContent!.trim()).toBe('2 of 3')
    click('[data-test="lightbox-next"]')
    await flushPromises()
    expect(name()).toBe('pic3.png')
    click('[data-test="lightbox-next"]')
    await flushPromises()
    expect(name()).toBe('pic1.png') // wraps around
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
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await flushPromises()
    expect(name()).toBe('pic1.png')
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
    expect(document.body.querySelector('[data-test="lightbox-counter"]')).toBeNull()
    click('[data-test="lightbox-close"]')
    await flushPromises()
    expect(lightbox()).toBeNull()
  })
})

describe('history', () => {
  it('loads earlier messages with the oldest id as the cursor', async () => {
    api.listMessages.mockResolvedValueOnce(Array.from({ length: 30 }, (_, i) => msg(100 - i)))
    api.listMessages.mockResolvedValueOnce([msg(69), msg(68)])
    const w = await mountPage()
    await openFirst(w)
    await w.find('[data-test="load-older"]').trigger('click')
    await flushPromises()
    expect(api.listMessages).toHaveBeenLastCalledWith(1, 71)
    expect(w.find('[data-message-id="68"]').exists()).toBe(true)
    expect(w.find('[data-test="load-older"]').exists()).toBe(false)
  })
})

describe('starting a chat', () => {
  it('lists the advisors with presence and opens the chosen private chat', async () => {
    api.openDirect.mockResolvedValue(conv(9, { peer: person({ userId: 3, name: 'Omar' }), unreadCount: 0, lastMessageId: null, lastMessagePreview: null }))
    api.listMessages.mockResolvedValue([])
    const w = await mountPage()
    await w.find('[data-test="new-chat"]').trigger('click')
    await flushPromises()
    const options = Array.from(document.body.querySelectorAll('[data-test="staff-option"]'))
    expect(options.map(o => o.textContent)).toEqual([expect.stringContaining('Jane'), expect.stringContaining('Omar')])
    expect(options[0]!.textContent).toContain('Online')
    ;(options[1] as HTMLElement).click()
    await flushPromises()
    expect(api.openDirect).toHaveBeenCalledWith(3)
    expect(w.find('[data-test="peer-name"]').text()).toBe('Omar')
  })
})

describe('closed conversations', () => {
  it('replaces the composer with a notice and a reopen action', async () => {
    api.listInbox.mockResolvedValue([conv(1, { status: 'CLOSED' })])
    api.openDirect.mockResolvedValue(conv(1, { status: 'OPEN' }))
    const w = await mountPage()
    await openFirst(w)
    expect(w.find('[data-test="closed"]').exists()).toBe(true)
    expect(w.find('[data-test="composer"]').exists()).toBe(false)
    await w.find('[data-test="closed"] button').trigger('click')
    await flushPromises()
    expect(api.openDirect).toHaveBeenCalledWith(2)
  })
})
