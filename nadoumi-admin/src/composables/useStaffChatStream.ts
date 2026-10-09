import { onBeforeUnmount, onMounted, ref } from 'vue'
import { API_BASE } from '@/utils/apiBase'
import type { ChatMessage, PresenceEvent, ReceiptEvent, RemovedEvent } from '@/api/conversation'

export const STAFF_STREAM_URL = `${API_BASE}/api/staff/stream`
export const STREAM_BACKOFF_START_MS = 1_000
export const STREAM_BACKOFF_MAX_MS = 30_000
export const STREAM_POLL_MS = 30_000

export interface ChatStreamHandlers {
  message: (message: ChatMessage) => void
  delivered: (event: ReceiptEvent) => void
  read: (event: ReceiptEvent) => void
  presence: (event: PresenceEvent) => void
  /** Another participant (a colleague) deleted the conversation. */
  removed: (event: RemovedEvent) => void
  /** After every reconnect (events sent while offline are lost) and, without EventSource, every {@link STREAM_POLL_MS}. */
  resync: () => void
}

/**
 * The live chat stream. The admin session is an httpOnly cookie and the stream is a plain GET through the same-origin
 * proxy, so a native EventSource authenticates with no header. Events carry their data (a new message, a receipt, a
 * presence change), so nothing is re-fetched because of them. Reconnects with exponential backoff and reports
 * `reconnecting` while the link is down.
 */
export function useStaffChatStream(handlers: ChatStreamHandlers) {
  const connected = ref(false)
  const reconnecting = ref(false)
  let source: EventSource | undefined
  let retryTimer: ReturnType<typeof setTimeout> | undefined
  let pollTimer: ReturnType<typeof setInterval> | undefined
  let delay = STREAM_BACKOFF_START_MS
  let stopped = true

  function listen<T>(name: string, fn: (payload: T) => void) {
    source?.addEventListener(name, (e: Event) => {
      try {
        fn(JSON.parse((e as MessageEvent<string>).data) as T)
      }
      catch {
        // a malformed event is ignored; the next resync catches up
      }
    })
  }

  function connect() {
    if (stopped) return
    source = new EventSource(STAFF_STREAM_URL, { withCredentials: true })
    source.addEventListener('open', () => {
      connected.value = true
      delay = STREAM_BACKOFF_START_MS
      if (reconnecting.value) {
        reconnecting.value = false
        handlers.resync()
      }
    })
    listen<ChatMessage>('message', handlers.message)
    listen<ReceiptEvent>('delivered', handlers.delivered)
    listen<ReceiptEvent>('read', handlers.read)
    listen<PresenceEvent>('presence', handlers.presence)
    listen<RemovedEvent>('removed', handlers.removed)
    source.addEventListener('error', () => {
      connected.value = false
      reconnecting.value = true
      source?.close()
      source = undefined
      if (stopped) return
      retryTimer = setTimeout(connect, delay)
      delay = Math.min(delay * 2, STREAM_BACKOFF_MAX_MS)
    })
  }

  function start() {
    if (!stopped) return
    stopped = false
    if (typeof EventSource === 'undefined') {
      pollTimer = setInterval(handlers.resync, STREAM_POLL_MS)
      return
    }
    connect()
  }

  function stop() {
    stopped = true
    connected.value = false
    reconnecting.value = false
    source?.close()
    source = undefined
    if (retryTimer) clearTimeout(retryTimer)
    if (pollTimer) clearInterval(pollTimer)
    retryTimer = undefined
    pollTimer = undefined
  }

  onMounted(start)
  onBeforeUnmount(stop)
  return { connected, reconnecting, start, stop }
}
