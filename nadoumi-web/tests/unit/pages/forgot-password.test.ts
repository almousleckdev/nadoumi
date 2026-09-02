import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Forgot from '~/pages/forgot-password.vue'

const fetchImpl = vi.fn((url: string, _opts?: Record<string, unknown>) => {
  if (url === '/api/student-email-otp') return Promise.resolve({ sent: true })
  if (url === '/api/student-email-otp-verify') return Promise.resolve({ ticket: 'tkt_7' })
  if (url === '/api/student-password-reset') return Promise.resolve(undefined)
  return Promise.resolve({})
})
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockClear())

describe('forgot-password (real flow)', () => {
  it('verify -> reset -> done posts the reset with the ticket', async () => {
    const w = await mountSuspended(Forgot)
    await w.find('#otp-email').setValue('sam@example.com')
    await w.find('button').trigger('click') // [Verify]
    await flushPromises()

    const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
    await flushPromises()

    await w.find('#reset-new').setValue('BrandNew1!')
    await w.find('#reset-confirm').setValue('BrandNew1!')
    await w.find('form').trigger('submit')
    await flushPromises()

    const call = fetchImpl.mock.calls.find(c => c[0] === '/api/student-password-reset')
    expect(call?.[1]).toMatchObject({ method: 'POST', body: { ticket: 'tkt_7', newPassword: 'BrandNew1!' } })
    expect(w.text().toLowerCase()).toContain('can now sign in')
  })

  it('blocks the reset when the new password fails the policy', async () => {
    const w = await mountSuspended(Forgot)
    await w.find('#otp-email').setValue('sam@example.com')
    await w.find('button').trigger('click')
    await flushPromises()
    const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue('1')
    await flushPromises()

    await w.find('#reset-new').setValue('weak')
    await w.find('#reset-confirm').setValue('weak')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(fetchImpl.mock.calls.some(c => c[0] === '/api/student-password-reset')).toBe(false)
  })
})
