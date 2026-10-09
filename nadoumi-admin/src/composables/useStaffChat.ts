import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  INBOX_PAGE_SIZE, MESSAGE_PAGE_SIZE, closeConversation, listInbox, listMessages, markConversationRead, openDirect,
  postMessage,
  type ChatAttachment, type ChatConversation, type ChatMessage, type PresenceEvent, type ReceiptEvent,
} from '@/api/conversation'
import { useStaffChatStream } from '@/composables/useStaffChatStream'
import { maxPointer, mergeMessages, sortInbox } from '@/utils/chat'

export type LoadStatus = 'loading' | 'ready' | 'error'

/** What a file that has just been attached looks like in the optimistic bubble. */
export interface OutgoingFile { name: string, type: string, size: number }

interface Outgoing { body: string, mediaIds: number[], files: OutgoingFile[] }

/** How a typed search is sent to the inbox: an application id narrows by application, anything else by name. */
export function inboxFilterFor(query: string): { q?: string, applicationId?: number } {
  const text = query.trim()
  if (!text) return {}
  const app = text.match(/^(?:app|application)[\s#:-]*(\d{1,18})$/i)
  return app ? { applicationId: Number(app[1]) } : { q: text }
}

/**
 * The staff chat as one state machine: the inbox, the open conversation, optimistic sends, receipts and presence.
 * REST loads pages; the SSE stream applies changes as they happen, so neither the inbox nor the thread is
 * re-fetched because a message arrived.
 */
export function useStaffChat(myId: () => number) {
  const inbox = ref<ChatConversation[]>([])
  const inboxStatus = ref<LoadStatus>('loading')
  const hasMoreInbox = ref(false)
  const loadingMoreInbox = ref(false)
  let inboxPage = 0
  let filter: { q?: string, applicationId?: number } = {}

  const activeId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const threadStatus = ref<LoadStatus>('ready')
  const hasOlder = ref(false)
  const loadingOlder = ref(false)
  const olderFailed = ref(false)
  const active = computed(() => inbox.value.find(c => c.id === activeId.value) ?? null)

  const outgoing = new Map<number, Outgoing>()
  let tempSeq = 0

  // ---- inbox ----

  async function loadInbox(options: { silent?: boolean, query?: string } = {}) {
    if (options.query !== undefined) filter = inboxFilterFor(options.query)
    if (!options.silent) inboxStatus.value = 'loading'
    try {
      const page = await listInbox({ ...filter, page: 0 }, true)
      if (options.silent) {
        const fresh = new Set(page.map(c => c.id))
        inbox.value = sortInbox([...page, ...inbox.value.filter(c => !fresh.has(c.id))])
      }
      else {
        inboxPage = 0
        inbox.value = sortInbox(page)
        hasMoreInbox.value = page.length === INBOX_PAGE_SIZE
      }
      inboxStatus.value = 'ready'
    }
    catch {
      if (!options.silent) inboxStatus.value = 'error'
    }
  }

  async function loadMoreInbox() {
    if (loadingMoreInbox.value || !hasMoreInbox.value) return
    loadingMoreInbox.value = true
    try {
      const page = await listInbox({ ...filter, page: inboxPage + 1 }, true)
      inboxPage += 1
      const known = new Set(inbox.value.map(c => c.id))
      inbox.value = sortInbox([...inbox.value, ...page.filter(c => !known.has(c.id))])
      hasMoreInbox.value = page.length === INBOX_PAGE_SIZE
    }
    catch {
      // the button stays; the staff member can try again
    }
    finally {
      loadingMoreInbox.value = false
    }
  }

  // ---- thread ----

  async function select(id: number | null) {
    activeId.value = id
    messages.value = []
    hasOlder.value = false
    olderFailed.value = false
    if (id === null) {
      threadStatus.value = 'ready'
      return
    }
    threadStatus.value = 'loading'
    try {
      const page = await listMessages(id, 0, true)
      if (activeId.value !== id) return
      messages.value = mergeMessages([], page)
      hasOlder.value = page.length === MESSAGE_PAGE_SIZE
      threadStatus.value = 'ready'
      acknowledge(id)
    }
    catch {
      if (activeId.value === id) threadStatus.value = 'error'
    }
  }

  async function loadOlder() {
    const oldest = messages.value.find(m => m.id > 0)
    const id = activeId.value
    if (!oldest || id === null || loadingOlder.value) return
    loadingOlder.value = true
    olderFailed.value = false
    try {
      const page = await listMessages(id, oldest.id, true)
      if (activeId.value !== id) return
      messages.value = mergeMessages(messages.value, page)
      hasOlder.value = page.length === MESSAGE_PAGE_SIZE
    }
    catch {
      olderFailed.value = true
    }
    finally {
      loadingOlder.value = false
    }
  }

  /** Marks the open conversation read on the server and zeroes its badge, only while someone is looking at it. */
  function acknowledge(conversationId: number) {
    const conversation = inbox.value.find(c => c.id === conversationId)
    if (!conversation || conversation.unreadCount === 0) return
    if (document.visibilityState === 'hidden') return
    conversation.unreadCount = 0
    markConversationRead(conversationId).catch(() => undefined)
  }

  // ---- sending ----

  function temporaryMessage(conversationId: number, body: string, files: OutgoingFile[]): ChatMessage {
    tempSeq += 1
    const attachments: ChatAttachment[] = files.map((f, i) => ({
      id: -(tempSeq * 10 + i), filename: f.name, contentType: f.type, byteSize: f.size, image: false,
    }))
    return {
      id: -tempSeq, conversationId, senderUserId: myId(), senderName: null, body,
      createdAt: new Date().toISOString().slice(0, 19), editedAt: null, attachments, sendState: 'sending',
    }
  }

  async function transmit(conversationId: number, temp: ChatMessage, pending: Outgoing) {
    try {
      const sent = await postMessage(conversationId, pending.body, pending.mediaIds)
      outgoing.delete(temp.id)
      if (activeId.value === conversationId) {
        messages.value = mergeMessages(messages.value.filter(m => m.id !== temp.id), [sent])
      }
      touchConversation(conversationId, sent, 0)
    }
    catch {
      const failed = messages.value.find(m => m.id === temp.id)
      if (failed) failed.sendState = 'failed'
    }
  }

  /** Shows the message immediately and confirms it in the background. */
  function send(body: string, mediaIds: number[] = [], files: OutgoingFile[] = []) {
    const conversationId = activeId.value
    const text = body.trim()
    if (conversationId === null || (!text && mediaIds.length === 0)) return
    const temp = temporaryMessage(conversationId, text, files)
    const pending: Outgoing = { body: text, mediaIds, files }
    outgoing.set(temp.id, pending)
    messages.value = mergeMessages(messages.value, [temp])
    void transmit(conversationId, temp, pending)
  }

  function retry(tempId: number) {
    const conversationId = activeId.value
    const pending = outgoing.get(tempId)
    const temp = messages.value.find(m => m.id === tempId)
    if (conversationId === null || !pending || !temp) return
    temp.sendState = 'sending'
    void transmit(conversationId, temp, pending)
  }

  function discard(tempId: number) {
    outgoing.delete(tempId)
    messages.value = messages.value.filter(m => m.id !== tempId)
  }

  function previewOf(message: ChatMessage): string {
    if (message.body.trim()) return message.body
    return message.attachments.length === 1 ? '📎' : `📎 ${message.attachments.length}`
  }

  /** Keeps the inbox row's preview, time and unread badge in step with a message that just arrived or was sent. */
  function touchConversation(conversationId: number, message: ChatMessage, unreadDelta: number) {
    const conversation = inbox.value.find(c => c.id === conversationId)
    if (!conversation) {
      void loadInbox({ silent: true })
      return
    }
    conversation.lastMessageId = message.id
    conversation.lastMessagePreview = message.body || message.attachments.length ? previewOf(message) : conversation.lastMessagePreview
    conversation.lastMessageAt = message.createdAt
    conversation.lastSenderUserId = message.senderUserId
    conversation.unreadCount += unreadDelta
    inbox.value = sortInbox(inbox.value)
  }

  // ---- starting and closing ----

  async function openWith(studentUserId: number): Promise<number> {
    const conversation = await openDirect(studentUserId)
    if (!inbox.value.some(c => c.id === conversation.id)) {
      inbox.value = sortInbox([conversation, ...inbox.value])
    }
    else {
      const known = inbox.value.find(c => c.id === conversation.id)!
      known.status = conversation.status
    }
    await select(conversation.id)
    return conversation.id
  }

  async function close(conversationId: number) {
    await closeConversation(conversationId)
    const conversation = inbox.value.find(c => c.id === conversationId)
    if (conversation) conversation.status = 'CLOSED'
  }

  // ---- live events ----

  function onMessage(message: ChatMessage) {
    const isActive = activeId.value === message.conversationId
    if (isActive) messages.value = mergeMessages(messages.value, [message])
    touchConversation(message.conversationId, message, 1)
    if (isActive) acknowledge(message.conversationId)
  }

  function onDelivered(event: ReceiptEvent) {
    const conversation = inbox.value.find(c => c.id === event.conversationId)
    if (conversation) conversation.peerDeliveredMessageId = maxPointer(conversation.peerDeliveredMessageId, event.messageId)
  }

  function onRead(event: ReceiptEvent) {
    const conversation = inbox.value.find(c => c.id === event.conversationId)
    if (!conversation) return
    conversation.peerReadMessageId = maxPointer(conversation.peerReadMessageId, event.messageId)
    conversation.peerDeliveredMessageId = maxPointer(conversation.peerDeliveredMessageId, event.messageId)
  }

  const presenceByUser = reactive<Record<number, PresenceEvent>>({})

  function onPresence(event: PresenceEvent) {
    presenceByUser[event.userId] = event
    for (const c of inbox.value) {
      if (c.peer?.userId === event.userId) {
        c.peer.online = event.online
        c.peer.lastSeenAt = event.online ? null : event.lastSeenAt
      }
    }
  }

  /** Events sent while the link was down are lost: pull what changed. */
  async function resync() {
    await loadInbox({ silent: true })
    const id = activeId.value
    if (id === null) return
    try {
      messages.value = mergeMessages(messages.value, await listMessages(id, 0, true))
      acknowledge(id)
    }
    catch {
      // the next resync or event catches up
    }
  }

  const { reconnecting } = useStaffChatStream({
    message: onMessage, delivered: onDelivered, read: onRead, presence: onPresence, resync: () => void resync(),
  })

  function onVisible() {
    if (document.visibilityState === 'visible' && activeId.value !== null) acknowledge(activeId.value)
  }
  onMounted(() => document.addEventListener('visibilitychange', onVisible))
  onBeforeUnmount(() => document.removeEventListener('visibilitychange', onVisible))

  return {
    inbox, inboxStatus, hasMoreInbox, loadingMoreInbox, activeId, active, messages, threadStatus, hasOlder,
    loadingOlder, olderFailed, reconnecting, presenceByUser,
    loadInbox, loadMoreInbox, select, loadOlder, send, retry, discard, openWith, close,
  }
}
