import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import PhotoUploadCard from '~/components/onboarding/PhotoUploadCard.vue'
import { fakeFile, pickFile } from '../helpers/files'

const { api, size } = vi.hoisted(() => ({
  size: { width: 800, height: 800 },
  api: { uploadPhoto: vi.fn(), photoUrl: vi.fn() },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))
vi.mock('~/utils/files', async (original) => ({
  ...(await original<typeof import('~/utils/files')>()),
  readAsDataUrl: async () => 'data:image/png;base64,AAAA',
  imageSize: async () => size,
  dataUrlToBlob: async () => new Blob(['x'], { type: 'image/jpeg' }),
}))

// the cropper's canvas work is out of scope here: it just hands back a cropped image
const ImageCropper = { template: '<button data-test="crop" @click="$emit(\'crop\', \'data:image/jpeg;base64,BBBB\')">crop</button>', emits: ['crop'] }

async function mountCard() {
  const w = await mountSuspended(PhotoUploadCard, { props: { applicantId: 1 }, global: { stubs: { ImageCropper } } })
  await flushPromises()
  return w
}

beforeEach(() => {
  size.width = 800
  size.height = 800
  api.uploadPhoto.mockReset().mockResolvedValue({ mediaId: 9 })
  api.photoUrl.mockReset().mockRejectedValue({ statusCode: 404 })
})

describe('PhotoUploadCard', () => {
  it('starts with no photo', async () => {
    const w = await mountCard()

    expect(w.text()).toContain('No photo')
    expect(w.text()).toContain('Required')
  })

  it('shows the saved photo and marks the card complete', async () => {
    api.photoUrl.mockResolvedValue({ url: 'https://signed.example/photo.jpg', expiresAt: 'x' })
    const w = await mountCard()

    expect(w.find('img').attributes('src')).toBe('https://signed.example/photo.jpg')
    expect(w.text()).toContain('Complete')
  })

  it('rejects a photo that is too small', async () => {
    size.width = 300
    const w = await mountCard()

    await pickFile(w, fakeFile('me.png', 'image/png'))
    await flushPromises()

    expect(w.text()).toContain('at least 400x400')
    expect(w.find('[data-test="crop"]').exists()).toBe(false)
  })

  it('crops, uploads and reports the change', async () => {
    const w = await mountCard()
    await pickFile(w, fakeFile('me.png', 'image/png'))
    await flushPromises()

    await w.find('[data-test="crop"]').trigger('click')
    await w.findAll('button').find(b => b.text() === 'Save photo')!.trigger('click')
    await flushPromises()

    expect(api.uploadPhoto).toHaveBeenCalledWith(1, expect.any(Blob))
    expect(w.emitted('changed')).toHaveLength(1)
  })

  it('shows an error and does not report a change when the upload fails', async () => {
    api.uploadPhoto.mockRejectedValue({ statusCode: 500 })
    const w = await mountCard()
    await pickFile(w, fakeFile('me.png', 'image/png'))
    await flushPromises()
    await w.find('[data-test="crop"]').trigger('click')

    await w.findAll('button').find(b => b.text() === 'Save photo')!.trigger('click')
    await flushPromises()

    expect(w.text()).toContain('Something went wrong')
    expect(w.emitted('changed')).toBeUndefined()
  })
})
