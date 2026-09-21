import { onBeforeUnmount, onMounted, ref } from 'vue'
import { API_BASE } from '@/utils/apiBase'

export const STAFF_STREAM_URL = `${API_BASE}/api/staff/stream`
export const STREAM_BACKOFF_START_MS = 1_000
export const STREAM_BACKOFF_MAX_MS = 30_000
export const STREAM_POLL_MS = 30_000

/**
 * Live conversation pings from `/api/staff/stream` (SSE). The admin session is an
 * httpOnly cookie and the stream is a plain GET through the same-origin proxy, so a
 * native EventSource authenticates without any header. A ping carries only the
 * conversation id, never the message body: `onPing` re-fetches over REST.
 *
 * - Reconnects with exponential backoff (EventSource's own retry gives up on some errors).
 * - `onResync` fires after every reconnect (pings sent while offline are lost) and, where
 *   EventSource does not exist, every {@link STREAM_POLL_MS} as a polling fallback.
 * - Closes on unmount.
 */
export function useStaffStream(onPing: (conversationId: number) => void, onResync: () => void) {
  const connected = ref(false)
  /** True between a dropped connection and the next successful open. */
  const reconnecting = ref(false)
  let source: EventSource | undefined
  let retryTimer: ReturnType<typeof setTimeout> | undefined
  let pollTimer: ReturnType<typeof setInterval> | undefined
  let delay = STREAM_BACKOFF_START_MS
  let stopped = true

  function handlePing(e: Event) {
    try {
      const { refId } = JSON.parse((e as MessageEvent<string>).data) as { refId: number }
      if (typeof refId === 'number') onPing(refId)
    }
    catch {
      // a malformed ping is ignored; the next resync catches up
    }
  }

  function connect() {
    if (stopped) return
    source = new EventSource(STAFF_STREAM_URL, { withCredentials: true })
    source.addEventListener('open', () => {
      connected.value = true
      delay = STREAM_BACKOFF_START_MS
      if (reconnecting.value) {
        reconnecting.value = false
        onResync()
      }
    })
    source.addEventListener('conversation', handlePing)
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
      pollTimer = setInterval(onResync, STREAM_POLL_MS)
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
