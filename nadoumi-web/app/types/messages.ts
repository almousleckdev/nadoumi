/** `/api/student/conversations` (nadoumi-communication). Mirrors the backend response records. */
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

export interface OpenConversationBody {
  applicantId: number
  applicationId?: number
  subject?: string
  body: string
}

/** Size of one message page; the backend returns newest-first pages of exactly this many. */
export const MESSAGE_PAGE_SIZE = 50
export const MESSAGE_MAX_LENGTH = 4000
