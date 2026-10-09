import request from '@/utils/request'
import { API_BASE } from '@/utils/apiBase'

/** `/api/staff/**` chat endpoints (nadoumi-communication). Mirrors the backend response records. */

/** How the other side appears: a first name and a photo, plus whether they are connected. */
export interface ChatPerson {
  userId: number
  name: string
  avatarUrl: string | null
  online: boolean
  /** ISO instant (UTC); null when online now or never seen. */
  lastSeenAt: string | null
}

export interface ChatConversation {
  id: number
  subject: string | null
  applicationId: number | null
  conversationType: string
  status: 'OPEN' | 'CLOSED'
  lastMessageId: number | null
  lastMessagePreview: string | null
  lastMessageAt: string | null
  lastSenderUserId: number | null
  unreadCount: number
  peer: ChatPerson | null
  /** The student has received messages up to this id. */
  peerDeliveredMessageId: number | null
  /** The student has read messages up to this id. */
  peerReadMessageId: number | null
}

export interface ChatAttachment {
  id: number
  filename: string | null
  contentType: string | null
  byteSize: number
  image: boolean
}

export type SendState = 'sent' | 'sending' | 'failed'

export interface ChatMessage {
  id: number
  conversationId: number
  senderUserId: number
  senderName: string | null
  body: string
  createdAt: string
  editedAt: string | null
  attachments: ChatAttachment[]
  /** Client-only: a message the server has not confirmed yet carries a non-positive temporary id. */
  sendState?: SendState
}

export type ReceiptStatus = SendState | 'delivered' | 'read'

/** A student found by a staff search: a reference, a first name and a photo, nothing more. */
export interface StudentResult {
  userId: number
  studentRef: string
  name: string
  avatarUrl: string | null
  online: boolean
  matchedApplicationId: number | null
}

/** Bodies of the SSE events (besides `message`, which is a {@link ChatMessage}). */
export interface ReceiptEvent { conversationId: number, userId: number, messageId: number }
export interface PresenceEvent { userId: number, online: boolean, lastSeenAt: string | null }

export const MESSAGE_PAGE_SIZE = 30
export const INBOX_PAGE_SIZE = 30
export const MESSAGE_MAX_LENGTH = 4000
export const MAX_ATTACHMENTS_PER_MESSAGE = 5
export const MESSAGE_ATTACHMENT_MAX_MB = 15
/** Mirrors the backend MESSAGE_ATTACHMENT policy; the server still enforces it. */
export const MESSAGE_ATTACHMENT_MIME = new RegExp(
  '^(image/(jpeg|png|webp)|application/pdf|text/plain|application/msword'
  + '|application/vnd\\.openxmlformats-officedocument\\.(wordprocessingml\\.document|spreadsheetml\\.sheet|presentationml\\.presentation))$',
)
export const MESSAGE_ATTACHMENT_ACCEPT = [
  'image/jpeg', 'image/png', 'image/webp', 'application/pdf', 'text/plain', 'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
].join(',')

export interface InboxParams {
  q?: string
  applicationId?: number
  page?: number
}

// ---- nad:conversation:participate ----

/** The caller's own conversations, newest activity first, one page at a time. */
export const listInbox = (params: InboxParams = {}, silent = false) =>
  request.get<unknown, ChatConversation[]>('/api/staff/conversations', { params, silent } as object)

/** Opens the private chat with a student (or reuses it) and posts the first message in one call. */
export const createConversation = (body: { studentUserId: number, body: string, applicationId?: number }) =>
  request.post<unknown, ChatMessage>('/api/staff/conversations', body)

/** Finds students by student id, application id or name; a handful of results. */
export const searchStudents = (q: string) =>
  request.get<unknown, StudentResult[]>('/api/staff/chat/students', { params: { q }, silent: true } as object)

/** Gets or creates the private chat with one student; nothing is posted. */
export const openDirect = (studentUserId: number) =>
  request.post<unknown, ChatConversation>('/api/staff/conversations/direct', { userId: studentUserId })

/** Newest-first page; pass the oldest id already shown as `beforeId` to page backwards (0 = newest). */
export const listMessages = (id: number, beforeId = 0, silent = false) =>
  request.get<unknown, ChatMessage[]>(`/api/staff/conversations/${id}/messages`, { params: { beforeId }, silent } as object)

export const postMessage = (id: number, body: string, attachmentMediaIds: number[] = []) =>
  request.post<unknown, ChatMessage>(`/api/staff/conversations/${id}/messages`, {
    body,
    attachmentMediaIds: attachmentMediaIds.length ? attachmentMediaIds : undefined,
  })

export const uploadAttachment = (conversationId: number, file: File): Promise<{ mediaId: number }> => {
  const form = new FormData()
  form.append('file', file)
  return request.post<unknown, { mediaId: number }>(`/api/staff/conversations/${conversationId}/attachments`, form)
}

export const markConversationRead = (id: number) =>
  request.post<unknown, undefined>(`/api/staff/conversations/${id}/read`)

export const closeConversation = (id: number) =>
  request.post<unknown, undefined>(`/api/staff/conversations/${id}/close`)

export const chatUnreadCount = () =>
  request.get<unknown, { count: number }>('/api/staff/conversations/unread-count', { silent: true } as object)

/** Same-origin URL that redirects to a short-lived signed link, for `<img src>` and download links. */
export function attachmentUrl(conversationId: number, attachmentId: number, download = false): string {
  return `${API_BASE}/api/staff/conversations/${conversationId}/attachments/${attachmentId}${download ? '?download=1' : ''}`
}
