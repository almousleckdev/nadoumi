import { describe, it, expect, vi, beforeEach } from 'vitest'

const documentContent = vi.hoisted(() => vi.fn())
vi.mock('@/api/document', () => ({ documentContent }))
const saveBlob = vi.hoisted(() => vi.fn())
vi.mock('@/utils/download', () => ({ saveBlob }))

import { extensionFor, openDocumentFile } from '@/utils/documentFile'

const open = vi.fn()
beforeEach(() => {
  documentContent.mockReset()
  saveBlob.mockReset()
  open.mockReset()
  vi.stubGlobal('open', open)
  vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn().mockReturnValue('blob:x'), revokeObjectURL: vi.fn() }))
})

describe('openDocumentFile', () => {
  it('opens the bytes in a new tab when viewing', async () => {
    documentContent.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    await openDocumentFile(3, 2, 'view')
    expect(documentContent).toHaveBeenCalledWith(3, 2, true)
    expect(open).toHaveBeenCalledWith('blob:x', '_blank', 'noopener')
    expect(saveBlob).not.toHaveBeenCalled()
  })

  it('saves the file with an extension taken from its type', async () => {
    documentContent.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    await openDocumentFile(3, 2, 'download')
    expect(documentContent).toHaveBeenCalledWith(3, 2, false)
    expect(saveBlob).toHaveBeenCalledWith(expect.any(Blob), 'document-3-v2.pdf')
    expect(open).not.toHaveBeenCalled()
  })

  it('falls back to the stored content type when the response type is generic', async () => {
    documentContent.mockResolvedValue(new Blob(['x'], { type: 'application/octet-stream' }))
    await openDocumentFile(3, 2, 'download', 'image/jpeg')
    expect(saveBlob).toHaveBeenCalledWith(expect.objectContaining({ type: 'image/jpeg' }), 'document-3-v2.jpg')
  })
})

describe('extensionFor', () => {
  it('ignores parameters and case, and returns nothing for an unknown type', () => {
    expect(extensionFor('IMAGE/PNG; charset=binary')).toBe('png')
    expect(extensionFor('application/x-unknown')).toBe('')
    expect(extensionFor(undefined)).toBe('')
  })
})
