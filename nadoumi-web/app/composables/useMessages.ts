import type { AttachmentAccess, ConversationMessage, ConversationSummary, OpenConversationBody } from '~/types/messages'

/** The signed-in student's own conversations, via the BFF proxy to `/api/student/conversations`. */
export function useMessages() {
  const { studentFetch } = useApi()

  return {
    listConversations: () => studentFetch<ConversationSummary[]>('conversations'),
    /** Newest-first page; pass the oldest id already shown as `beforeId` to page backwards. */
    listMessages: (conversationId: number, beforeId = 0) =>
      studentFetch<ConversationMessage[]>(`conversations/${conversationId}/messages`, { query: { beforeId } }),
    /** `attachmentMediaIds` are ids already returned by `uploadAttachment` — upload-then-attach. */
    post: (conversationId: number, body: string, attachmentMediaIds: number[] = []) =>
      studentFetch<ConversationMessage>(`conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { body, attachmentMediaIds: attachmentMediaIds.length ? attachmentMediaIds : undefined },
      }),
    open: (body: OpenConversationBody) =>
      studentFetch<ConversationMessage>('conversations', { method: 'POST', body }),
    markRead: (conversationId: number) =>
      studentFetch<undefined>(`conversations/${conversationId}/read`, { method: 'POST' }),
    /** Stores the file and returns its media id, ready to pass to `post`. */
    uploadAttachment: (conversationId: number, file: File) => {
      const form = new FormData()
      form.append('file', file, file.name)
      return studentFetch<{ mediaId: number }>(`conversations/${conversationId}/attachments`, { method: 'POST', body: form })
    },
    /** A short-lived signed URL to view/download an already-sent attachment. */
    attachmentAccess: (conversationId: number, attachmentId: number) =>
      studentFetch<AttachmentAccess>(`conversations/${conversationId}/attachments/${attachmentId}`, { query: { json: 1 } }),
  }
}
