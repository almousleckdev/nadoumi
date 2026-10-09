import { useState } from '#imports'

/**
 * The number on the navigation badge. One cheap request refreshes it from anywhere; while the messages page is
 * open the chat keeps it current from the inbox it already holds, so the number never lags the screen.
 */
export function useChatUnread() {
  const { studentFetch } = useApi()
  const count = useState<number>('chat-unread', () => 0)

  async function refresh() {
    try {
      count.value = (await studentFetch<{ count: number }>('conversations/unread-count')).count
    }
    catch {
      // a badge that cannot refresh keeps its last value
    }
  }

  return { count, refresh }
}
