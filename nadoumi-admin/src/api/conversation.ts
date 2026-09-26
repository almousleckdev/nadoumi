import request from '@/utils/request'

/** `/api/staff/conversations` (nadoumi-communication). Mirrors the backend response records. */
export interface ConversationSummary {
  id: number
  subject: string | null
  applicationId: number | null
  conversationType: string
  status: 'OPEN' | 'CLOSED'
  lastMessagePreview: string | null
  lastMessageAt: string | null
  unreadCount: number
}

export interface MessageAttachment {
  id: number
  mediaAssetId: number
  filename: string | null
  contentType: string | null
  byteSize: number
  url?: string | null
}

export interface ConversationMessage {
  id: number
  conversationId: number
  senderUserId: number
  senderName: string | null
  body: string
  createdAt: string
  editedAt: string | null
  attachments: MessageAttachment[]
}

export interface Participant {
  userId: number
  role: 'STAFF' | 'APPLICANT' | 'AGENT' | 'GUARDIAN'
  addedAt: string
}

/** Size of one message page; the backend returns newest-first pages of exactly this many. */
export const MESSAGE_PAGE_SIZE = 50
export const MESSAGE_MAX_LENGTH = 4000

// ---- nad:conversation:participate ----
/** The caller's own conversations plus OPEN ones no staff member has joined yet. */
export const listInbox = () =>
  request.get<unknown, ConversationSummary[]>('/api/staff/conversations')

/** Newest-first page; pass the oldest id already shown as `beforeId` to page backwards (0 = newest). */
export const listMessages = (id: number, beforeId = 0, silent = false) =>
  request.get<unknown, ConversationMessage[]>(`/api/staff/conversations/${id}/messages`, { params: { beforeId }, silent } as object)

export const postMessage = (id: number, body: string, attachmentMediaIds: number[] = []) =>
  request.post<unknown, ConversationMessage>(`/api/staff/conversations/${id}/messages`, {
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

export const listParticipants = (id: number) =>
  request.get<unknown, Participant[]>(`/api/staff/conversations/${id}/participants`)

export const closeConversation = (id: number) =>
  request.post<unknown, undefined>(`/api/staff/conversations/${id}/close`)

// ---- nad:conversation:participant:manage ----
export const addParticipant = (id: number, body: { userId: number, role: Participant['role'] }) =>
  request.post<unknown, undefined>(`/api/staff/conversations/${id}/participants`, body)
