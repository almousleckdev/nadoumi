import type { ConversationMessage, ConversationSummary } from '~/types/messages'

/**
 * Union of two message lists, unique by id, oldest first. Used to fold a freshly
 * fetched page (live ping, resync, or a just-sent reply) into what is on screen
 * without duplicating or reordering anything already shown.
 */
export function mergeMessages(existing: ConversationMessage[], incoming: ConversationMessage[]): ConversationMessage[] {
  const byId = new Map<number, ConversationMessage>()
  for (const m of existing) byId.set(m.id, m)
  for (const m of incoming) byId.set(m.id, m)
  return [...byId.values()].sort((a, b) => a.id - b.id)
}

/** Time for today's items, short date for older ones. `iso` is the backend's zone-less local timestamp. */
export function formatMessageTime(iso: string | null, locale: string, now: Date = new Date()): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const sameDay = date.toDateString() === now.toDateString()
  return new Intl.DateTimeFormat(locale, sameDay ? { timeStyle: 'short' } : { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export function filterConversations(items: ConversationSummary[], query: string): ConversationSummary[] {
  const q = query.trim().toLowerCase()
  if (!q) return items
  return items.filter((c) => {
    const haystack = [c.subject, c.adminName, c.lastMessagePreview, c.applicationId ? String(c.applicationId) : '']
    return haystack.some(field => (field || '').toLowerCase().includes(q))
  })
}
