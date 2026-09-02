import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Contact from '~/pages/contact.vue'

const { publicPost } = vi.hoisted(() => ({ publicPost: vi.fn() }))
// keep the real problemMessage; only swap the useApi composable
vi.mock('~/composables/useApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('~/composables/useApi')>()),
  useApi: () => ({ publicGet: vi.fn(), publicPost, studentFetch: vi.fn() }),
}))

beforeEach(() => publicPost.mockReset())

async function fill(w: Awaited<ReturnType<typeof mountSuspended>>) {
  await w.find('#c-name').setValue('Dana Ali')
  await w.find('#c-email').setValue('dana@example.com')
  await w.find('#c-message').setValue('Do you support the September intake?')
}

describe('contact page', () => {
  it('validates required fields before posting', async () => {
    const w = await mountSuspended(Contact)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(publicPost).not.toHaveBeenCalled()
    expect(w.text()).toContain('This field is required.')
  })

  it('posts the message and shows the success alert', async () => {
    publicPost.mockResolvedValueOnce(undefined)
    const w = await mountSuspended(Contact)
    await fill(w)
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(publicPost).toHaveBeenCalledWith('contact', expect.objectContaining({
      name: 'Dana Ali',
      email: 'dana@example.com',
      message: 'Do you support the September intake?',
    }))
    expect(w.text()).toContain('Message sent')
  })

  it('surfaces a server error without losing the form', async () => {
    publicPost.mockImplementationOnce(() => Promise.reject(
      Object.assign(new Error('rate limited'), { data: { detail: 'Too many attempts' } }),
    ))
    const w = await mountSuspended(Contact)
    await fill(w)
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(w.text()).toContain('Too many attempts')
    expect(w.find('#c-name').exists()).toBe(true)
  })
})
