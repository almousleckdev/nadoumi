/** `/api/student/conversations` and the chat stream (nadoumi-communication). Mirrors the backend response records. */

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
  /** The other side has received messages up to this id. */
  peerDeliveredMessageId: number | null
  /** The other side has read messages up to this id. */
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
  /** Client-only: a message not yet confirmed by the server carries a negative temporary id. */
  sendState?: SendState
}

export type ReceiptStatus = SendState | 'delivered' | 'read'

/** Bodies of the SSE events (besides `message`, which is a {@link ChatMessage}). */
export interface ReceiptEvent { conversationId: number, userId: number, messageId: number }
export interface PresenceEvent { userId: number, online: boolean, lastSeenAt: string | null }

export const MESSAGE_PAGE_SIZE = 30
export const INBOX_PAGE_SIZE = 30
export const MESSAGE_MAX_LENGTH = 4000
export const MAX_ATTACHMENTS_PER_MESSAGE = 5
