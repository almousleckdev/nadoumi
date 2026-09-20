import type { NotificationView, Page } from '~/types/catalog'

/** The signed-in student's own notification feed, via the BFF proxy to `/api/notifications`. */
export function useNotifications() {
  function list(params: { unreadOnly?: boolean, page?: number, size?: number } = {}) {
    return $fetch<Page<NotificationView>>('/api/student-notifications', { query: params })
  }
  function unreadCount() {
    return $fetch<{ count: number }>('/api/student-notifications/unread-count')
  }
  function markRead(id: number) {
    return $fetch<{ updated: boolean }>(`/api/student-notifications/${id}/read`, { method: 'POST' })
  }
  function markAllRead() {
    return $fetch<{ updated: number }>('/api/student-notifications/read-all', { method: 'POST' })
  }
  return { list, unreadCount, markRead, markAllRead }
}
