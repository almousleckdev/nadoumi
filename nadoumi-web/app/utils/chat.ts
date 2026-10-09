import type { ChatConversation, ChatMessage, ReceiptStatus } from '~/types/chat'

/** Union of two message lists, unique by id (the incoming copy wins), oldest first. Temporary (negative) ids sort last. */
export function mergeMessages(existing: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const byId = new Map<number, ChatMessage>()
  for (const m of existing) byId.set(m.id, m)
  for (const m of incoming) byId.set(m.id, m)
  const real = [...byId.values()].filter(m => m.id > 0).sort((a, b) => a.id - b.id)
  const temporary = [...byId.values()].filter(m => m.id <= 0).sort((a, b) => b.id - a.id)
  return [...real, ...temporary]
}

/** Sent, delivered or read: derived from the other side's pointers, so no per-message state is stored. */
export function receiptStatus(message: ChatMessage, conversation: Pick<ChatConversation, 'peerDeliveredMessageId' | 'peerReadMessageId'> | null): ReceiptStatus {
  if (message.sendState === 'sending' || message.sendState === 'failed') return message.sendState
  if (conversation?.peerReadMessageId != null && conversation.peerReadMessageId >= message.id) return 'read'
  if (conversation?.peerDeliveredMessageId != null && conversation.peerDeliveredMessageId >= message.id) return 'delivered'
  return 'sent'
}

/** A new inbox row ordering: newest activity first. */
export function sortInbox(items: ChatConversation[]): ChatConversation[] {
  return [...items].sort((a, b) => (b.lastMessageAt ?? '').localeCompare(a.lastMessageAt ?? '') || b.id - a.id)
}

/** Receipt pointers only ever move forward. */
export function maxPointer(current: number | null, next: number): number {
  return current == null || next > current ? next : current
}

export interface DayGroup { key: string, date: Date, messages: ChatMessage[] }

/** Splits an oldest-first list into calendar days, for the date dividers. */
export function groupByDay(messages: ChatMessage[]): DayGroup[] {
  const groups: DayGroup[] = []
  for (const m of messages) {
    const date = new Date(m.createdAt)
    const key = Number.isNaN(date.getTime()) ? 'unknown' : `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    const last = groups[groups.length - 1]
    if (last && last.key === key) last.messages.push(m)
    else groups.push({ key, date, messages: [m] })
  }
  return groups
}

const MINUTE_MS = 60_000
const HOUR_MS = 60 * MINUTE_MS
const DAY_MS = 24 * HOUR_MS
/** Consecutive messages from one sender closer than this share a bubble cluster (no repeated tail). */
export const CLUSTER_GAP_MS = 5 * MINUTE_MS

export function sameCluster(a: ChatMessage | undefined, b: ChatMessage): boolean {
  if (!a || a.senderUserId !== b.senderUserId) return false
  const gap = new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  return gap >= 0 && gap < CLUSTER_GAP_MS
}

/** "5 minutes ago", "yesterday", or a date: localised by Intl, so no strings to translate. */
export function relativeTime(iso: string | null, locale: string, now: Date = new Date()): string {
  if (!iso) return ''
  const then = new Date(iso)
  if (Number.isNaN(then.getTime())) return ''
  const diff = then.getTime() - now.getTime()
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
  const abs = Math.abs(diff)
  if (abs < MINUTE_MS) return rtf.format(0, 'second')
  if (abs < HOUR_MS) return rtf.format(Math.round(diff / MINUTE_MS), 'minute')
  if (abs < DAY_MS) return rtf.format(Math.round(diff / HOUR_MS), 'hour')
  if (abs < 7 * DAY_MS) return rtf.format(Math.round(diff / DAY_MS), 'day')
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(then)
}

/** Clock time for today, short date otherwise: the inbox timestamp. */
export function inboxTime(iso: string | null, locale: string, now: Date = new Date()): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const sameDay = date.toDateString() === now.toDateString()
  return new Intl.DateTimeFormat(locale, sameDay ? { timeStyle: 'short' } : { dateStyle: 'short' }).format(date)
}

export function clockTime(iso: string, locale: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat(locale, { timeStyle: 'short' }).format(date)
}

/** Divider label: Today / Yesterday from Intl, a full date after that. */
export function dayLabel(date: Date, locale: string, now: Date = new Date()): string {
  if (Number.isNaN(date.getTime())) return ''
  const startOf = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const days = Math.round((startOf(date) - startOf(now)) / DAY_MS)
  if (days === 0 || days === -1) return new Intl.RelativeTimeFormat(locale, { numeric: 'auto' }).format(days, 'day')
  return new Intl.DateTimeFormat(locale, { dateStyle: 'full' }).format(date)
}

const KB = 1024
export function formatBytes(bytes: number, locale: string): string {
  const fmt = (n: number, unit: string) => `${new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(n)} ${unit}`
  if (bytes < KB) return fmt(bytes, 'B')
  if (bytes < KB * KB) return fmt(bytes / KB, 'KB')
  return fmt(bytes / (KB * KB), 'MB')
}

/** Short badge text for a file card: PDF, DOC, XLS... */
export function fileKind(filename: string | null, contentType: string | null): string {
  const ext = filename?.split('.').pop()?.toLowerCase() ?? ''
  if (ext && ext.length <= 4 && ext !== (filename ?? '').toLowerCase()) return ext.toUpperCase()
  if (contentType === 'application/pdf') return 'PDF'
  return 'FILE'
}

/** Case-insensitive filter over what the inbox shows: the other person's name, the preview and the application id. */
export function filterInbox(items: ChatConversation[], query: string): ChatConversation[] {
  const q = query.trim().toLowerCase()
  if (!q) return items
  return items.filter(c => [c.peer?.name, c.subject, c.lastMessagePreview, c.applicationId ? String(c.applicationId) : '']
    .some(field => (field ?? '').toLowerCase().includes(q)))
}
