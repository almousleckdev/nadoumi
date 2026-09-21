/** `/api/student/support/tickets` shapes. Student-facing: priority and assignee are never sent. */
export const TICKET_STATUSES = ['OPEN', 'IN_PROGRESS', 'WAITING_ON_STUDENT', 'RESOLVED', 'CLOSED'] as const
export type TicketStatus = (typeof TICKET_STATUSES)[number]

export const TICKET_CATEGORIES = ['ACCOUNT', 'APPLICATION', 'DOCUMENT', 'PAYMENT', 'TECHNICAL', 'OTHER'] as const
export type TicketCategory = (typeof TICKET_CATEGORIES)[number]

export interface TicketSummary {
  id: number
  subject: string
  category: TicketCategory
  status: TicketStatus
  createTime: string
  updateTime: string
}

export interface TicketAttachment {
  id: number
  mediaAssetId: number
  filename: string | null
  contentType: string | null
  byteSize: number
}

export interface TicketMessage {
  id: number
  conversationId: number
  senderUserId: number
  senderName: string | null
  body: string
  createdAt: string
  editedAt: string | null
  attachments: TicketAttachment[]
}

export interface TicketDetail {
  ticket: TicketSummary
  conversationId: number
  messages: TicketMessage[]
}

export interface CreateTicketBody {
  subject: string
  category: TicketCategory
  body: string
}
