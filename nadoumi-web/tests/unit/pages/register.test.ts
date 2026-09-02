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

async function reachPasswordStep(w: VueWrapper) {
  await w.find('#firstName').setValue('Ada')
  await w.find('#lastName').setValue('Lovelace')
  await w.find('#otp-email').setValue('ada@example.com')
  await w.find('button').trigger('click') // [Verify]
  await flushPromises()
  const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
  for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
  await flushPromises()
  await new Promise(r => setTimeout(r, 700)) // "email verified" -> emits verified
  await flushPromises()
}

describe('register wizard (3 steps)', () => {
  it('keeps Verify disabled until both names and a valid email are present', async () => {
    const w = await mountSuspended(Register)
    const verify = () => w.findAll('button').find(b => b.text().toLowerCase() === 'verify')!
    expect(verify().attributes('disabled')).toBeDefined()

    await w.find('#firstName').setValue('Ada')
    await w.find('#lastName').setValue('Lovelace')
    await w.find('#otp-email').setValue('not-an-email')
    expect(verify().attributes('disabled')).toBeDefined()

    await w.find('#otp-email').setValue('ada@example.com')
    expect(verify().attributes('disabled')).toBeUndefined()
  })

  it('has no confirm-email field', async () => {
    const w = await mountSuspended(Register)
    expect(w.find('#confirmEmail').exists()).toBe(false)
  })

  it('gates Next on verified + strong password + match + consent, then posts and routes to onboarding', async () => {
    const w = await mountSuspended(Register)
    await reachPasswordStep(w)

    const next = () => w.findAll('button').find(b => b.text().toLowerCase() === 'next')!
    expect(next().attributes('disabled')).toBeDefined()

    await w.find('#password').setValue('Abcdef1!')
    await w.find('#confirm').setValue('Abcdef1!')
    expect(next().attributes('disabled')).toBeDefined() // consent unchecked

    await w.find('#accept-terms').setValue(true)
    expect(next().attributes('disabled')).toBeUndefined()

    await w.find('form').trigger('submit')
    await flushPromises()

    const call = fetchImpl.mock.calls.find(c => c[0] === '/api/student-account')
    expect(call?.[1]).toMatchObject({
      method: 'POST',
      body: expect.objectContaining({ firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com', ticket: 'tkt_1' }),
    })
    expect(nav).toHaveBeenCalledWith('/onboarding')
  })

  it('a password containing the name never enables Next', async () => {
    const w = await mountSuspended(Register)
    await reachPasswordStep(w)
    await w.find('#password').setValue('Lovelace1!')
    await w.find('#confirm').setValue('Lovelace1!')
    await w.find('#accept-terms').setValue(true)
    const next = w.findAll('button').find(b => b.text().toLowerCase() === 'next')!
    expect(next.attributes('disabled')).toBeDefined()
  })
})
