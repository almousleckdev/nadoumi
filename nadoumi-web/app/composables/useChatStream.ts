import { ref, onMounted, onBeforeUnmount } from 'vue'
import type { ChatMessage, PresenceEvent, ReceiptEvent } from '~/types/chat'

export const CHAT_STREAM_URL = '/api/student-stream'
export const STREAM_BACKOFF_START_MS = 1_000
export const STREAM_BACKOFF_MAX_MS = 30_000
export const STREAM_POLL_MS = 30_000

export interface ChatStreamHandlers {
  message: (message: ChatMessage) => void
  delivered: (event: ReceiptEvent) => void
  read: (event: ReceiptEvent) => void
  presence: (event: PresenceEvent) => void
  /** After every reconnect (events sent while offline are lost) and, without EventSource, every {@link STREAM_POLL_MS}. */
  resync: () => void
}

/**
 * The live chat stream: new messages, receipts and presence arrive as events carrying their data, so nothing is
 * re-fetched. Reconnects with exponential backoff (EventSource's own retry gives up on some failures, such as a
 * 401 from an expired session) and reports `reconnecting` while the link is down. Client-only; a no-op in SSR.
 */
export function useChatStream(handlers: ChatStreamHandlers) {
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
    source = new EventSource(CHAT_STREAM_URL)
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
    if (!import.meta.client || !stopped) return
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
