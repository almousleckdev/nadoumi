import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import Register from '~/pages/register.vue'

const { nav } = vi.hoisted(() => ({ nav: vi.fn() }))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

const fetchImpl = vi.fn((url: string, _opts?: Record<string, unknown>) => {
  if (url === '/api/student-email-otp') return Promise.resolve({ sent: true })
  if (url === '/api/student-email-otp-verify') return Promise.resolve({ ticket: 'tkt_1' })
  if (url === '/api/student-account') return Promise.resolve({ signedIn: true })
  if (url === '/api/student-session') {
    return Promise.resolve({
      authenticated: true,
      user: { userId: 1, username: 'stu_a', nickName: 'Ada Lovelace', email: 'ada@example.com' },
      applicants: [],
    })
  }
  return Promise.resolve({})
})
vi.stubGlobal('$fetch', fetchImpl)

beforeEach(() => {
  fetchImpl.mockClear()
  nav.mockReset()
})

async function completeStepOne(w: VueWrapper) {
  await w.find('#firstName').setValue('Ada')
  await w.find('#lastName').setValue('Lovelace')
  await w.find('#otp-email').setValue('ada@example.com')
  await w.find('button').trigger('click') // [Verify]
  await flushPromises()
  const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
  for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
  await flushPromises()
}

describe('register page (two-step)', () => {
  it('verifies the email in step 1 then posts the account with the ticket', async () => {
    const w = await mountSuspended(Register)
    await completeStepOne(w)

    await w.find('#password').setValue('Abcdef1!')
    await w.find('#confirm').setValue('Abcdef1!')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()

    const accountCalls = fetchImpl.mock.calls.filter(c => c[0] === '/api/student-account')
    expect(accountCalls).toHaveLength(1)
    expect(accountCalls[0]?.[1]).toMatchObject({
      method: 'POST',
      body: expect.objectContaining({
        firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com', ticket: 'tkt_1',
      }),
    })
    expect(nav).toHaveBeenCalledWith('/dashboard/profile')
  })

  it('blocks the step-2 submit when the password fails the policy', async () => {
    const w = await mountSuspended(Register)
    await completeStepOne(w)

    await w.find('#password').setValue('weak')
    await w.find('#confirm').setValue('weak')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(fetchImpl.mock.calls.some(c => c[0] === '/api/student-account')).toBe(false)
    expect(w.text()).toContain('at least 8 characters')
  })
})
