import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h, reactive } from 'vue'
import { mount } from '@vue/test-utils'
import { testI18n } from '../helpers'

const http = vi.hoisted(() => ({ post: vi.fn() }))
vi.mock('@/utils/request', () => ({ default: { post: http.post } }))

const message = vi.hoisted(() => ({ warning: vi.fn(), error: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, warning: message.warning, error: message.error } }
})

import { useUniversityGallery } from '@/views/universities/useUniversityGallery'
import { blankUniversityForm } from '@/views/universities/universityForm'

const png = (name = 'a.png') => new File(['x'], name, { type: 'image/png' })

function setup(id?: number) {
  const form = reactive(blankUniversityForm())
  form.id = id
  let api!: ReturnType<typeof useUniversityGallery>
  mount(
    defineComponent({
      setup() {
        api = useUniversityGallery(form)
        return () => h('div')
      },
    }),
    { global: { plugins: [testI18n()] } },
  )
  return { form, api }
}

describe('useUniversityGallery', () => {
  beforeEach(() => {
    http.post.mockReset()
    message.warning.mockReset()
    message.error.mockReset()
    URL.createObjectURL = vi.fn(() => 'blob:held')
    URL.revokeObjectURL = vi.fn()
  })

  it('uploads each picked file straight away for a saved university', async () => {
    http.post
      .mockResolvedValueOnce({ data: { mediaId: 11, url: 'https://cdn/1.jpg' } })
      .mockResolvedValueOnce({ data: { mediaId: 12, url: 'https://cdn/2.jpg' } })
    const { form, api } = setup(5)
    await api.addFiles([png('1.png'), png('2.png')])
    expect(http.post).toHaveBeenCalledWith('/api/staff/universities/5/gallery', expect.any(FormData))
    expect(form.gallery.map(g => g.mediaId)).toEqual([11, 12])
    expect(form.gallery[0]).toMatchObject({ url: 'https://cdn/1.jpg', imageUrl: 'https://cdn/1.jpg' })
  })

  it('holds picked files locally until the university exists', async () => {
    const { form, api } = setup()
    await api.addFiles([png()])
    expect(http.post).not.toHaveBeenCalled()
    expect(form.gallery).toHaveLength(1)
    expect(form.gallery[0]).toMatchObject({ mediaId: null, url: 'blob:held' })
    expect(form.gallery[0]!._file).toBeInstanceOf(File)
  })

  it('skips a failed upload without adding a row', async () => {
    http.post.mockRejectedValueOnce(new Error('boom'))
    const { form, api } = setup(5)
    await api.addFiles([png()])
    expect(form.gallery).toEqual([])
  })

  it('trims the pick to the remaining room and warns', async () => {
    const { form, api } = setup()
    for (let i = 0; i < 9; i++) form.gallery.push({ imageUrl: null, mediaId: i + 1, url: null, caption: null })
    await api.addFiles([png('a.png'), png('b.png'), png('c.png')])
    expect(form.gallery).toHaveLength(10)
    expect(message.warning).toHaveBeenCalled()
  })

  it('does nothing when the gallery is already full', async () => {
    const { form, api } = setup()
    for (let i = 0; i < 10; i++) form.gallery.push({ imageUrl: null, mediaId: i + 1, url: null, caption: null })
    await api.addFiles([png()])
    expect(form.gallery).toHaveLength(10)
    expect(message.warning).not.toHaveBeenCalled()
  })

  it('rejects files that are not images', async () => {
    const { form, api } = setup()
    await api.addFiles([new File(['x'], 'a.pdf', { type: 'application/pdf' })])
    expect(form.gallery).toEqual([])
    expect(message.error).toHaveBeenCalled()
  })

  it('rejects images larger than the size cap', async () => {
    const big = png()
    Object.defineProperty(big, 'size', { value: 6 * 1024 * 1024 })
    const { form, api } = setup()
    await api.addFiles([big])
    expect(form.gallery).toEqual([])
    expect(message.error).toHaveBeenCalled()
  })

  it('uploads held rows after the university is created and reports success', async () => {
    http.post.mockResolvedValueOnce({ data: { mediaId: 21, url: 'https://cdn/x.jpg' } })
    const { form, api } = setup()
    await api.addFiles([png()])
    const tasks = api.heldUploadTasks(9)
    expect(tasks).toHaveLength(1)
    expect(await tasks[0]!()).toBe(true)
    expect(http.post).toHaveBeenCalledWith('/api/staff/universities/9/gallery', expect.any(FormData))
    expect(form.gallery[0]).toMatchObject({ mediaId: 21, url: 'https://cdn/x.jpg', imageUrl: 'https://cdn/x.jpg', _file: null })
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:held')
  })

  it('reports a held row that fails to upload', async () => {
    http.post.mockRejectedValueOnce(new Error('boom'))
    const { form, api } = setup()
    await api.addFiles([png()])
    expect(await api.heldUploadTasks(9)[0]!()).toBe(false)
    expect(form.gallery[0]!.mediaId).toBeNull()
  })

  it('has no upload tasks when nothing is held', () => {
    const { form, api } = setup(5)
    form.gallery.push({ imageUrl: null, mediaId: 1, url: null, caption: null })
    expect(api.heldUploadTasks(5)).toEqual([])
  })

  it('releases the preview url when a held row is removed', async () => {
    const { form, api } = setup()
    await api.addFiles([png()])
    api.removeRow(0)
    expect(form.gallery).toEqual([])
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:held')
  })
})
