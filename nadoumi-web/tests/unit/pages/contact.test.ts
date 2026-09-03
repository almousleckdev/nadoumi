import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Contact from '~/pages/contact.vue'

const { publicPost } = vi.hoisted(() => ({ publicPost: vi.fn() }))
vi.mock('~/composables/useApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('~/composables/useApi')>()),
  useApi: () => ({ publicGet: vi.fn(), publicPost, studentFetch: vi.fn() }),
}))

beforeEach(() => publicPost.mockReset())

async function fill(w: Awaited<ReturnType<typeof mountSuspended>>) {
  await w.find('#c-first').setValue('Dana')
  await w.find('#c-last').setValue('Ali')
  await w.find('#c-email').setValue('dana@example.com')
  await w.find('#c-message').setValue('Do you support the September intake?')
}

describe('contact page', () => {
  it('validates the required fields before posting', async () => {
    const w = await mountSuspended(Contact)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(publicPost).not.toHaveBeenCalled()
    expect(w.findAll('[role="alert"]').some(n => n.text().includes('This field is required.'))).toBe(true)
  })

  it('posts first/last name + message and shows the success alert', async () => {
    publicPost.mockResolvedValueOnce(undefined)
    const w = await mountSuspended(Contact)
    await fill(w)
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(publicPost).toHaveBeenCalledWith('contact', expect.objectContaining({
      firstName: 'Dana',
      lastName: 'Ali',
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
    expect(w.find('#c-first').exists()).toBe(true)
  })

  it('shows contact-info blocks with honest "to be supplied" placeholders', async () => {
    const w = await mountSuspended(Contact)
    expect(w.text()).toContain('support@nadoumi.com')
    expect(w.text()).toContain('to be supplied')
  })
})
