import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'
import ImageUpload from '@/components/ui/ImageUpload.vue'

const message = vi.hoisted(() => ({ error: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, error: message.error } }
})

type Exposed = {
  onSuccess: (res: unknown) => void
  clear: () => void
}

function mountUpload(props: Record<string, unknown> = {}) {
  return mount(ImageUpload, {
    props: { modelValue: null, action: '/api/staff/universities/12/logo', ...props },
    ...mountOpts(),
  })
}

describe('ImageUpload', () => {
  beforeEach(() => message.error.mockReset())

  it('emits the media id and shows the fresh url on a successful upload', async () => {
    const w = mountUpload()
    ;(w.vm as unknown as Exposed).onSuccess({ mediaId: 42, url: 'https://res.cloudinary.com/x.png' })
    await flushPromises()

    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([42])
    expect(w.find('img').attributes('src')).toBe('https://res.cloudinary.com/x.png')
  })

  it('emits null when cleared', async () => {
    const w = mountUpload({ modelValue: 42, previewUrl: 'https://res.cloudinary.com/x.png' })
    ;(w.vm as unknown as Exposed).clear()
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([null])
  })

  it('surfaces a problem+json body instead of emitting', () => {
    const w = mountUpload()
    ;(w.vm as unknown as Exposed).onSuccess({ detail: 'File is too large', status: 413 })
    expect(w.emitted('update:modelValue')).toBeUndefined()
    expect(message.error).toHaveBeenCalledWith('File is too large')
  })

  it('renders the disabled hint and no dropzone while disabled', () => {
    const w = mountUpload({ disabled: true, disabledHint: 'Save first' })
    expect(w.text()).toContain('Save first')
    expect(w.find('.el-upload').exists()).toBe(false)
  })
})
