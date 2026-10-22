import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import SignatureCard from '~/components/applicant/SignatureCard.vue'

const api = vi.hoisted(() => ({ status: vi.fn(), image: vi.fn(), save: vi.fn() }))
mockNuxtImport('useApplicantSignature', () => () => api)

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  URL.createObjectURL = vi.fn(() => 'blob:sig')
  URL.revokeObjectURL = vi.fn()
})

async function mountCard() {
  const w = await mountSuspended(SignatureCard, { props: { applicantId: 3 } })
  await flushPromises()
  return w
}

describe('SignatureCard', () => {
  it('asks for a signature and reports not ready until one is on file', async () => {
    api.status.mockResolvedValue({ signed: false, signedAt: null })
    const w = await mountCard()

    expect(w.find('[data-test="pad"]').exists()).toBe(true)
    expect(w.find('[data-test="save-signature"]').attributes('disabled')).toBeDefined() // nothing drawn yet
    expect(w.emitted('ready')?.at(-1)).toEqual([false])
  })

  it('shows the signature on file and reports ready', async () => {
    api.status.mockResolvedValue({ signed: true, signedAt: '2026-10-22T10:00:00' })
    api.image.mockResolvedValue(new Blob(['x'], { type: 'image/png' }))
    const w = await mountCard()

    expect(w.find('[data-test="signed"]').exists()).toBe(true)
    expect(w.find('img').attributes('src')).toBe('blob:sig')
    expect(w.emitted('ready')?.at(-1)).toEqual([true])
  })

  it('uploads a picture of the signature and then reads the new state', async () => {
    api.status.mockResolvedValueOnce({ signed: false, signedAt: null }).mockResolvedValueOnce({ signed: true, signedAt: 'now' })
    api.image.mockResolvedValue(new Blob(['x'], { type: 'image/png' }))
    api.save.mockResolvedValue({ signed: true, signedAt: 'now' })
    const w = await mountCard()

    const file = new File(['png'], 'sig.png', { type: 'image/png' })
    const input = w.find('[data-test="upload-signature"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    expect(api.save).toHaveBeenCalledWith(file)
    expect(w.emitted('ready')?.at(-1)).toEqual([true])
  })

  it('refuses a file that is not a PNG or JPEG without calling the server', async () => {
    api.status.mockResolvedValue({ signed: false, signedAt: null })
    const w = await mountCard()

    const file = new File(['gif'], 'sig.gif', { type: 'image/gif' })
    const input = w.find('[data-test="upload-signature"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    expect(api.save).not.toHaveBeenCalled()
    expect(w.text()).toContain('PNG or JPEG')
  })
})
