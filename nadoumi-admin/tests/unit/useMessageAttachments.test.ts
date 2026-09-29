import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { testI18n } from '../helpers'

const api = vi.hoisted(() => ({ uploadAttachment: vi.fn() }))
vi.mock('@/api/conversation', () => api)

const message = vi.hoisted(() => ({ warning: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, warning: message.warning } }
})

import { MAX_ATTACHMENTS, useMessageAttachments } from '@/views/conversations/useMessageAttachments'

const file = (name: string, type: string, size = 100) => {
  const f = new File(['x'], name, { type })
  Object.defineProperty(f, 'size', { value: size })
  return f
}

function setup(conversationId: number | null = 7) {
  const id = ref<number | null>(conversationId)
  let api!: ReturnType<typeof useMessageAttachments>
  mount(
    defineComponent({
      setup() {
        api = useMessageAttachments(id)
        return () => h('div')
      },
    }),
    { global: { plugins: [testI18n()] } },
  )
  return api
}

describe('useMessageAttachments', () => {
  beforeEach(() => {
    api.uploadAttachment.mockReset()
    message.warning.mockReset()
  })

  it('uploads a picked file and keeps its media id', async () => {
    api.uploadAttachment.mockResolvedValueOnce({ mediaId: 55 })
    const att = setup()
    await att.addFiles([file('cv.pdf', 'application/pdf')])
    expect(api.uploadAttachment).toHaveBeenCalledWith(7, expect.any(File))
    expect(att.pending.value).toHaveLength(1)
    expect(att.pending.value[0]).toMatchObject({ mediaId: 55, uploading: false, error: '' })
    expect(att.readyIds.value).toEqual([55])
    expect(att.isUploading.value).toBe(false)
  })

  it('marks an attachment as failed when the upload rejects', async () => {
    api.uploadAttachment.mockRejectedValueOnce(new Error('boom'))
    const att = setup()
    await att.addFiles([file('cv.pdf', 'application/pdf')])
    expect(att.pending.value[0]!.error).not.toBe('')
    expect(att.readyIds.value).toEqual([])
  })

  it('reports uploading while the request is in flight', async () => {
    let resolve!: (v: { mediaId: number }) => void
    api.uploadAttachment.mockReturnValueOnce(new Promise((r) => { resolve = r }))
    const att = setup()
    const done = att.addFiles([file('cv.pdf', 'application/pdf')])
    await flushPromises()
    expect(att.isUploading.value).toBe(true)
    resolve({ mediaId: 1 })
    await done
    expect(att.isUploading.value).toBe(false)
  })

  it('skips files larger than the size cap with a warning', async () => {
    const att = setup()
    await att.addFiles([file('huge.pdf', 'application/pdf', 16 * 1024 * 1024)])
    expect(att.pending.value).toEqual([])
    expect(api.uploadAttachment).not.toHaveBeenCalled()
    expect(message.warning).toHaveBeenCalled()
  })

  it('skips files of a type that is not allowed', async () => {
    const att = setup()
    await att.addFiles([file('run.exe', 'application/x-msdownload')])
    expect(att.pending.value).toEqual([])
    expect(message.warning).toHaveBeenCalled()
  })

  it('accepts an allowed extension when the browser gives no mime type', async () => {
    api.uploadAttachment.mockResolvedValueOnce({ mediaId: 3 })
    const att = setup()
    await att.addFiles([file('letter.docx', '')])
    expect(att.readyIds.value).toEqual([3])
  })

  it('stops at the attachment limit and warns', async () => {
    api.uploadAttachment.mockResolvedValue({ mediaId: 1 })
    const att = setup()
    const many = Array.from({ length: MAX_ATTACHMENTS + 2 }, (_, i) => file(`f${i}.pdf`, 'application/pdf'))
    await att.addFiles(many)
    expect(att.pending.value).toHaveLength(MAX_ATTACHMENTS)
    expect(message.warning).toHaveBeenCalled()
    message.warning.mockReset()
    await att.addFiles([file('extra.pdf', 'application/pdf')])
    expect(att.pending.value).toHaveLength(MAX_ATTACHMENTS)
    expect(message.warning).toHaveBeenCalled()
  })

  it('does nothing without a selected conversation', async () => {
    const att = setup(null)
    await att.addFiles([file('cv.pdf', 'application/pdf')])
    expect(att.pending.value).toEqual([])
    expect(api.uploadAttachment).not.toHaveBeenCalled()
  })

  it('removes a pending attachment by key and can clear them all', async () => {
    api.uploadAttachment.mockResolvedValue({ mediaId: 1 })
    const att = setup()
    await att.addFiles([file('a.pdf', 'application/pdf'), file('b.pdf', 'application/pdf')])
    att.remove(att.pending.value[0]!.key)
    expect(att.pending.value).toHaveLength(1)
    att.clear()
    expect(att.pending.value).toEqual([])
  })
})
