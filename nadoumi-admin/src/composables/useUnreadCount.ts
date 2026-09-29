import { onBeforeUnmount, onMounted, ref } from 'vue'
import { myUnreadCount } from '@/api/notification'

export const UNREAD_POLL_MS = 60_000

export function useUnreadCount() {
  const unread = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  async function refresh() {
    try {
      unread.value = (await myUnreadCount()).count
    }
    catch {
      /* silent — the bell is non-critical */
    }
  }

  function clear() {
    unread.value = 0
  }

  onMounted(() => {
    refresh()
    timer = setInterval(refresh, UNREAD_POLL_MS)
  })
  onBeforeUnmount(() => {
    if (timer) clearInterval(timer)
  })

  return { unread, refresh, clear }
}
