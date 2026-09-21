import type { ConversationMessage, ConversationSummary, OpenConversationBody } from '~/types/messages'

/** The signed-in student's own conversations, via the BFF proxy to `/api/student/conversations`. */
export function useMessages() {
  const { studentFetch } = useApi()

  return {
    listConversations: () => studentFetch<ConversationSummary[]>('conversations'),
    /** Newest-first page; pass the oldest id already shown as `beforeId` to page backwards. */
    listMessages: (conversationId: number, beforeId = 0) =>
      studentFetch<ConversationMessage[]>(`conversations/${conversationId}/messages`, { query: { beforeId } }),
    post: (conversationId: number, body: string) =>
      studentFetch<ConversationMessage>(`conversations/${conversationId}/messages`, { method: 'POST', body: { body } }),
    open: (body: OpenConversationBody) =>
      studentFetch<ConversationMessage>('conversations', { method: 'POST', body }),
    markRead: (conversationId: number) =>
      studentFetch<undefined>(`conversations/${conversationId}/read`, { method: 'POST' }),
  }
}
