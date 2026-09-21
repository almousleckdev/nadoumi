import type { NotificationView, Page } from '~/types/catalog'

/** The signed-in student's own notification feed, via the BFF proxy to `/api/notifications`. */
export function useNotifications() {
  // See useApi.ts: useRequestFetch() forwards the incoming request's cookies during
  // SSR, which the global $fetch does not.
  const requestFetch = useRequestFetch()

  function list(params: { unreadOnly?: boolean, page?: number, size?: number } = {}) {
    return requestFetch<Page<NotificationView>>('/api/student-notifications', { query: params })
  }
  function unreadCount() {
    return requestFetch<{ count: number }>('/api/student-notifications/unread-count')
  }
  function markRead(id: number) {
    return requestFetch<{ updated: boolean }>(`/api/student-notifications/${id}/read`, { method: 'POST' })
  }
  function markAllRead() {
    return requestFetch<{ updated: number }>('/api/student-notifications/read-all', { method: 'POST' })
  }
  return { list, unreadCount, markRead, markAllRead }
}
