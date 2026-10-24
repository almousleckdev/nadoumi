import { describe, it, expect, vi, beforeEach } from 'vitest'

const documentContent = vi.hoisted(() => vi.fn())
vi.mock('@/api/document', () => ({ documentContent }))

import { openDocumentFile } from '@/utils/documentFile'

const open = vi.fn()
beforeEach(() => {
  documentContent.mockReset()
  open.mockReset()
  vi.stubGlobal('open', open)
})

describe('openDocumentFile', () => {
  it('opens a signed URL in a new tab when viewing', async () => {
    documentContent.mockResolvedValue(new Blob([JSON.stringify({ url: 'https://signed.example/f' })], { type: 'application/json' }))
    await openDocumentFile(3, 2, 'view')
    expect(documentContent).toHaveBeenCalledWith(3, 2, true)
    expect(open).toHaveBeenCalledWith('https://signed.example/f', '_blank', 'noopener')
  })

  it('asks for a download link, not an inline one, when downloading', async () => {
    documentContent.mockResolvedValue(new Blob([JSON.stringify({ url: 'https://signed.example/f' })], { type: 'application/json' }))
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    await openDocumentFile(3, 2, 'download')
    expect(documentContent).toHaveBeenCalledWith(3, 2, false)
    expect(open).not.toHaveBeenCalled()
    expect(click).toHaveBeenCalledOnce()
    click.mockRestore()
  })

  it('refuses a non-http(s) URL rather than opening it', async () => {
    documentContent.mockResolvedValue(new Blob([JSON.stringify({ url: 'javascript:alert(1)' })], { type: 'application/json' }))
    await openDocumentFile(3, 2)
    expect(open).not.toHaveBeenCalled()
  })

  it('saves streamed bytes as a file named after the document and version', async () => {
    documentContent.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn().mockReturnValue('blob:x'), revokeObjectURL: vi.fn() }))

    await openDocumentFile(3, 2)

    expect(click).toHaveBeenCalledOnce()
    expect(open).not.toHaveBeenCalled()
    click.mockRestore()
  })
})
