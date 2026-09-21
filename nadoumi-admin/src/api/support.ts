import request from '@/utils/request'

export const TICKET_STATUSES = ['OPEN', 'IN_PROGRESS', 'WAITING_ON_STUDENT', 'RESOLVED', 'CLOSED'] as const
export type TicketStatus = (typeof TICKET_STATUSES)[number]
export const TICKET_PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT'] as const
export type TicketPriority = (typeof TICKET_PRIORITIES)[number]
export const TICKET_CATEGORIES = ['ACCOUNT', 'APPLICATION', 'DOCUMENT', 'PAYMENT', 'TECHNICAL', 'OTHER'] as const
export type TicketCategory = (typeof TICKET_CATEGORIES)[number]

/** `/api/staff/support/tickets` list row. Mirrors StaffTicketSummary. */
export interface StaffTicketSummary {
  id: number
  subject: string
  category: TicketCategory
  priority: TicketPriority
  status: TicketStatus
  openedByUserId: number
  applicantId: number | null
  assignedStaffId: number | null
  createTime: string
  updateTime: string
}

export interface TicketMessage {
  id: number
  conversationId: number
  senderUserId: number
  senderName: string | null
  body: string
  createdAt: string
  editedAt: string | null
  attachments: { id: number, mediaAssetId: number, filename: string | null, contentType: string | null, byteSize: number }[]
}

export interface TicketEvent {
  eventType: 'OPENED' | 'STATUS_CHANGED' | 'ASSIGNED' | 'REASSIGNED' | 'PRIORITY_CHANGED' | 'CATEGORY_CHANGED'
  oldValue: string | null
  newValue: string | null
  actorUserId: number
  createdAt: string
}

export interface StaffTicketDetail {
  ticket: StaffTicketSummary
  conversationId: number
  messages: TicketMessage[]
  events: TicketEvent[]
}

export interface TicketQueueParams {
  status?: TicketStatus
  priority?: TicketPriority
  category?: TicketCategory
  assigneeId?: number
  page?: number
  size?: number
}

const base = '/api/staff/support/tickets'

// The queue returns a plain array (no total), so the list screen pages by "is there another full page".
export const listTickets = (params: TicketQueueParams) =>
  request.get<unknown, StaffTicketSummary[]>(base, { params })

// needs nad:support:ticket:view
export const getTicket = (id: number) => request.get<unknown, StaffTicketDetail>(`${base}/${id}`)

// need nad:support:ticket:manage
export const changeTicketStatus = (id: number, status: TicketStatus) =>
  request.patch<unknown, StaffTicketSummary>(`${base}/${id}/status`, { status })
export const changeTicketPriority = (id: number, priority: TicketPriority) =>
  request.patch<unknown, StaffTicketSummary>(`${base}/${id}/priority`, { priority })
export const changeTicketCategory = (id: number, category: TicketCategory) =>
  request.patch<unknown, StaffTicketSummary>(`${base}/${id}/category`, { category })
export const replyToTicket = (id: number, body: string) =>
  request.post<unknown, TicketMessage>(`${base}/${id}/messages`, { body })

// needs nad:support:ticket:assign
export const assignTicket = (id: number, assigneeUserId: number) =>
  request.patch<unknown, StaffTicketSummary>(`${base}/${id}/assign`, { assigneeUserId })
