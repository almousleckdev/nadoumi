import request from '@/utils/request'
import type { Page } from './applicant'

export interface NotificationView {
  id: number
  type: string
  title: string
  body: string
  dataJson: string | null
  applicationId: number | null
  conversationId: number | null
  messageId: number | null
  createdAt: string
  readAt: string | null
  read: boolean
}

export interface NotificationDelivery {
  id: number
  channel: string
  provider: string | null
  providerMessageId: string | null
  status: string
  attempts: number
  lastError: string | null
  sentAt: string | null
}

export interface NotificationDetail {
  id: number
  recipientUserId: number
  type: string
  title: string
  body: string
  dataJson: string | null
  createdAt: string
  readAt: string | null
  deliveries: NotificationDelivery[]
}

// ---- staff oversight (nad:notification:list / :view) ----
export const listStaffNotifications = (params: {
  recipientUserId?: number
  type?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<NotificationView>>('/api/staff/notifications', { params })

export const getStaffNotification = (id: number) =>
  request.get<unknown, NotificationDetail>(`/api/staff/notifications/${id}`)

// ---- the caller's own feed ----
export const listMyNotifications = (params: { unreadOnly?: boolean, page?: number, size?: number }) =>
  request.get<unknown, Page<NotificationView>>('/api/notifications', { params })

export const myUnreadCount = () =>
  request.get<unknown, { count: number }>('/api/notifications/unread-count')

export const markNotificationRead = (id: number) =>
  request.post<unknown, { updated: boolean }>(`/api/notifications/${id}/read`)

export const markAllNotificationsRead = () =>
  request.post<unknown, { updated: number }>('/api/notifications/read-all')
