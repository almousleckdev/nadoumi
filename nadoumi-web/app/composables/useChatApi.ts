import type { ChatConversation, ChatMessage, ChatPerson } from '~/types/chat'

/** The signed-in student's chat endpoints, through the BFF proxy to `/api/student/conversations`. */
export function useChatApi() {
  const { studentFetch } = useApi()

  return {
    listInbox: (page = 0) => studentFetch<ChatConversation[]>('conversations', { query: { page } }),
    /** The staff a student can start a private chat with. */
    listStaff: () => studentFetch<ChatPerson[]>('conversations/staff'),
    /** Gets or creates the private chat with one staff member; nothing is sent. */
    openDirect: (staffUserId: number) =>
      studentFetch<ChatConversation>('conversations/direct', { method: 'POST', body: { userId: staffUserId } }),
    /** Newest-first page; pass the oldest id already shown as `beforeId` to page backwards. */
    listMessages: (conversationId: number, beforeId = 0) =>
      studentFetch<ChatMessage[]>(`conversations/${conversationId}/messages`, { query: { beforeId } }),
    /** `attachmentMediaIds` are ids already returned by `uploadAttachment` (upload, then attach). */
    post: (conversationId: number, body: string, attachmentMediaIds: number[] = []) =>
      studentFetch<ChatMessage>(`conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { body, attachmentMediaIds: attachmentMediaIds.length ? attachmentMediaIds : undefined },
      }),
    markRead: (conversationId: number) =>
      studentFetch<undefined>(`conversations/${conversationId}/read`, { method: 'POST' }),
    uploadAttachment: (conversationId: number, file: File) => {
      const form = new FormData()
      form.append('file', file, file.name)
      return studentFetch<{ mediaId: number }>(`conversations/${conversationId}/attachments`, { method: 'POST', body: form })
    },
  }
}

/** Same-origin URL that redirects to a short-lived signed link, for `<img src>` and download links. */
export function attachmentUrl(conversationId: number, attachmentId: number, download = false): string {
  return `/api/student-attachment/${conversationId}/${attachmentId}${download ? '?download=1' : ''}`
}
