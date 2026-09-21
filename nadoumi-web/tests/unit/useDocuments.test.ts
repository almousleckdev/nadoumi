import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useDocuments } from '~/composables/useDocuments'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

const raw = vi.fn()
const plainFetch = vi.fn()
vi.stubGlobal('$fetch', Object.assign(plainFetch, { raw }))

beforeEach(() => {
  studentFetch.mockReset()
  raw.mockReset()
  plainFetch.mockReset()
})

function blobResponse(contentType: string, body: string, disposition?: string) {
  const headers = new Headers({ 'content-type': contentType })
  if (disposition) headers.set('content-disposition', disposition)
  return { headers, _data: new Blob([body], { type: contentType }) }
}

describe('useDocuments', () => {
  it('lists the documents of one applicant', async () => {
    studentFetch.mockResolvedValueOnce([{ id: 1 }])
    const rows = await useDocuments().list(7)
    expect(studentFetch).toHaveBeenCalledWith('documents', { query: { applicantId: 7, applicationId: undefined } })
    expect(rows).toEqual([{ id: 1 }])
  })

  it('creates a document with the file, applicant and type as a multipart form', async () => {
    studentFetch.mockResolvedValueOnce({ id: 2 })
    const file = new File(['x'], 'transcript.pdf', { type: 'application/pdf' })
    await useDocuments().create(7, 'TRANSCRIPT', file)

    const [path, opts] = studentFetch.mock.calls[0]!
    expect(path).toBe('documents')
    expect(opts.method).toBe('POST')
    const form = opts.body as FormData
    expect(form.get('applicantId')).toBe('7')
    expect(form.get('docType')).toBe('TRANSCRIPT')
    expect((form.get('file') as File).name).toBe('transcript.pdf')
    expect(form.has('applicationId')).toBe(false)
  })

  it('uploads a replacement as a new version of the same document', async () => {
    studentFetch.mockResolvedValueOnce({ id: 2 })
    await useDocuments().replace(2, new File(['x'], 'v2.pdf', { type: 'application/pdf' }))
    const [path, opts] = studentFetch.mock.calls[0]!
    expect(path).toBe('documents/2/versions')
    expect(opts.method).toBe('POST')
  })

  it('deletes a draft document', async () => {
    studentFetch.mockResolvedValueOnce(undefined)
    await useDocuments().remove(2)
    expect(studentFetch).toHaveBeenCalledWith('documents/2', { method: 'DELETE' })
  })

  it('reads a signed URL when the backend answers with JSON', async () => {
    raw.mockResolvedValueOnce(blobResponse('application/json', JSON.stringify({ url: 'https://signed.example/f', expiresAt: 'x' })))
    const access = await useDocuments().fileAccess(2, 1)
    expect(raw).toHaveBeenCalledWith('/api/student/documents/2/versions/1/content', { query: { json: 1 }, responseType: 'blob' })
    expect(access).toEqual({ kind: 'url', url: 'https://signed.example/f' })
  })

  it('returns the streamed bytes and their filename for a sensitive document', async () => {
    raw.mockResolvedValueOnce(blobResponse('application/pdf', '%PDF', 'attachment; filename="passport.pdf"'))
    const access = await useDocuments().fileAccess(2, 1)
    expect(access.kind).toBe('blob')
    if (access.kind === 'blob') expect(access.filename).toBe('passport.pdf')
  })

  it('reads the configurable document types from the BFF dictionary route', async () => {
    plainFetch.mockResolvedValueOnce([{ value: 'PASSPORT', label: 'Passport' }])
    const types = await useDocuments().types()
    expect(plainFetch).toHaveBeenCalledWith('/api/student-document-types')
    expect(types).toEqual([{ value: 'PASSPORT', label: 'Passport' }])
  })
})
