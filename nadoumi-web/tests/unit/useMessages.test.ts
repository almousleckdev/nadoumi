import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useMessages } from '~/composables/useMessages'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

beforeEach(() => studentFetch.mockReset())

describe('useMessages', () => {
  it('GET conversations', async () => {
    studentFetch.mockResolvedValueOnce([{ id: 1 }])
    const rows = await useMessages().listConversations()
    expect(studentFetch).toHaveBeenCalledWith('conversations')
    expect(rows).toEqual([{ id: 1 }])
  })

  it('pages messages backwards with the beforeId cursor (0 = newest page)', async () => {
    studentFetch.mockResolvedValue([])
    await useMessages().listMessages(9)
    await useMessages().listMessages(9, 41)
    expect(studentFetch).toHaveBeenNthCalledWith(1, 'conversations/9/messages', { query: { beforeId: 0 } })
    expect(studentFetch).toHaveBeenNthCalledWith(2, 'conversations/9/messages', { query: { beforeId: 41 } })
  })

  it('POSTs a message body only', async () => {
    studentFetch.mockResolvedValueOnce({ id: 5 })
    await useMessages().post(9, 'hello')
    expect(studentFetch).toHaveBeenCalledWith('conversations/9/messages', {
      method: 'POST', body: { body: 'hello', attachmentMediaIds: undefined },
    })
  })

  it('POSTs a message with attachment media ids', async () => {
    studentFetch.mockResolvedValueOnce({ id: 5 })
    await useMessages().post(9, 'see attached', [101, 102])
    expect(studentFetch).toHaveBeenCalledWith('conversations/9/messages', {
      method: 'POST', body: { body: 'see attached', attachmentMediaIds: [101, 102] },
    })
  })

  it('uploads an attachment as multipart form data and returns the media id', async () => {
    studentFetch.mockResolvedValueOnce({ mediaId: 55 })
    const file = new File(['x'], 'scan.pdf', { type: 'application/pdf' })
    const result = await useMessages().uploadAttachment(9, file)

    expect(result).toEqual({ mediaId: 55 })
    const [path, opts] = studentFetch.mock.calls[0]!
    expect(path).toBe('conversations/9/attachments')
    expect(opts.method).toBe('POST')
    expect(opts.body).toBeInstanceOf(FormData)
    const sent = (opts.body as FormData).get('file') as File
    expect(sent.name).toBe('scan.pdf')
    expect(sent.type).toBe('application/pdf')
  })

  it('fetches a signed URL for an attachment', async () => {
    studentFetch.mockResolvedValueOnce({ url: 'https://cdn.example.com/x.pdf', expiresAt: '2026-01-01', filename: 'x.pdf', contentType: 'application/pdf' })
    const access = await useMessages().attachmentAccess(9, 500)
    expect(studentFetch).toHaveBeenCalledWith('conversations/9/attachments/500', { query: { json: 1 } })
    expect(access.url).toBe('https://cdn.example.com/x.pdf')
  })

  it('opens a conversation for an applicant', async () => {
    studentFetch.mockResolvedValueOnce({ id: 1, conversationId: 12 })
    await useMessages().open({ applicantId: 3, subject: 'Visa', body: 'Question' })
    expect(studentFetch).toHaveBeenCalledWith('conversations', {
      method: 'POST', body: { applicantId: 3, subject: 'Visa', body: 'Question' },
    })
  })

  it('marks a conversation read', async () => {
    studentFetch.mockResolvedValueOnce(undefined)
    await useMessages().markRead(9)
    expect(studentFetch).toHaveBeenCalledWith('conversations/9/read', { method: 'POST' })
  })
})
