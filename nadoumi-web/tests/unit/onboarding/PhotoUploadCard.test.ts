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
}))

async function mountCard() {
  const w = await mountSuspended(PhotoUploadCard, { props: { applicantId: 1 } })
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

  it('collapses to a compact summary once a photo is saved, and Replace expands it', async () => {
    api.photoUrl.mockResolvedValue({ url: 'https://signed.example/photo.jpg', expiresAt: 'x' })
    const w = await mountCard()

    expect(w.findAll('button').find(b => b.text() === 'Replace')).toBeTruthy()
    expect(w.find('input[type="file"]').exists()).toBe(false)

    await w.findAll('button').find(b => b.text() === 'Replace')!.trigger('click')

    expect(w.findAll('button').find(b => b.text() === 'Replace')).toBeUndefined()
    expect(w.find('input[type="file"]').exists()).toBe(true)
  })

  it('rejects a photo that is too small', async () => {
    size.width = 300
    const w = await mountCard()

    await pickFile(w, fakeFile('me.png', 'image/png'))
    await flushPromises()

    expect(w.text()).toContain('at least 400x400')
    expect(w.findAll('button').find(b => b.text() === 'Save photo')).toBeUndefined()
  })

  it('previews and uploads the picked file as-is, and reports the change', async () => {
    const w = await mountCard()
    const file = fakeFile('me.png', 'image/png')
    await pickFile(w, file)
    await flushPromises()

    expect(w.find('img').attributes('src')).toBe('data:image/png;base64,AAAA')

    await w.findAll('button').find(b => b.text() === 'Save photo')!.trigger('click')
    await flushPromises()

    expect(api.uploadPhoto).toHaveBeenCalledWith(1, file)
    expect(w.emitted('changed')).toHaveLength(1)
  })

  it('shows an error and does not report a change when the upload fails', async () => {
    api.uploadPhoto.mockRejectedValue({ statusCode: 500 })
    const w = await mountCard()
    await pickFile(w, fakeFile('me.png', 'image/png'))
    await flushPromises()

    await w.findAll('button').find(b => b.text() === 'Save photo')!.trigger('click')
    await flushPromises()

    expect(w.text()).toContain('Something went wrong')
    expect(w.emitted('changed')).toBeUndefined()
  })
})
