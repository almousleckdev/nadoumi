import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import Register from '~/pages/register.vue'

const { nav } = vi.hoisted(() => ({ nav: vi.fn() }))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

let usernameAnswer = () => ({ valid: true, available: true })
const USERNAME_SETTLE_MS = 500

const fetchImpl = vi.fn((url: string, _opts?: Record<string, unknown>) => {
  if (url === '/api/student-username') return Promise.resolve(usernameAnswer())
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
  usernameAnswer = () => ({ valid: true, available: true })
})

async function typeUsername(w: VueWrapper, value: string) {
  await w.find('#username').setValue(value)
  await new Promise(r => setTimeout(r, USERNAME_SETTLE_MS)) // the live check waits for typing to pause
  await flushPromises()
}

describe('register: username and email (names come later, from the passport)', () => {
  it('asks for a username and an email, not for names', async () => {
    const w = await mountSuspended(Register)

    expect(w.find('#username').exists()).toBe(true)
    expect(w.find('#firstName').exists()).toBe(false)
    expect(w.find('#lastName').exists()).toBe(false)
    expect(w.text()).not.toContain('exactly as they appear on your passport')
  })

  it('lower-cases the username and strips spaces as it is typed', async () => {
    const w = await mountSuspended(Register)

    await w.find('#username').setValue('Ada L')

    expect((w.find('#username').element as HTMLInputElement).value).toBe('adal')
  })

  it('says at once what is wrong with a username, without asking the server', async () => {
    const w = await mountSuspended(Register)

    await typeUsername(w, 'ab')

    expect(w.find('[data-test="username-feedback"]').text()).toContain('at least 3')
    // (an earlier test's pending check may still fire, so look for this handle specifically)
    expect(fetchImpl.mock.calls.some(c => c[0] === '/api/student-username' && (c[1] as { query: { username: string } }).query.username === 'ab')).toBe(false)
  })

  it('checks a well-formed username with the server and shows that it is available', async () => {
    const w = await mountSuspended(Register)

    await typeUsername(w, 'ada_l')

    expect(fetchImpl.mock.calls.find(c => c[0] === '/api/student-username')?.[1]).toMatchObject({ query: { username: 'ada_l' } })
    expect(w.find('[data-test="username-feedback"]').text()).toContain('available')
  })

  it('shows a taken username and keeps Verify disabled', async () => {
    usernameAnswer = () => ({ valid: true, available: false })
    const w = await mountSuspended(Register)
    await w.find('#otp-email').setValue('ada@example.com')

    await typeUsername(w, 'ada_l')

    expect(w.find('[data-test="username-feedback"]').text()).toContain('already taken')
    const verify = w.findAll('button').find(b => b.text().toLowerCase() === 'verify')!
    expect(verify.attributes('disabled')).toBeDefined()
  })

  it('does not block Verify when the check answers with an error envelope (RuoYi 200 + code 401)', async () => {
    usernameAnswer = () => ({ code: 401, msg: 'not authenticated' }) as never
    const w = await mountSuspended(Register)
    await w.find('#otp-email').setValue('ada@example.com')

    await typeUsername(w, 'ada_l')

    const verify = w.findAll('button').find(b => b.text().toLowerCase() === 'verify')!
    expect(verify.attributes('disabled')).toBeUndefined()
  })

  it('does not block Verify when the check request itself fails', async () => {
    usernameAnswer = () => { throw new Error('down') }
    const w = await mountSuspended(Register)
    await w.find('#otp-email').setValue('ada@example.com')

    await typeUsername(w, 'ada_l')

    const verify = w.findAll('button').find(b => b.text().toLowerCase() === 'verify')!
    expect(verify.attributes('disabled')).toBeUndefined()
  })
})

async function reachPasswordStep(w: VueWrapper) {
  await typeUsername(w, 'ada_l')
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
  it('keeps Verify disabled until the username is usable and the email is valid', async () => {
    const w = await mountSuspended(Register)
    const verify = () => w.findAll('button').find(b => b.text().toLowerCase() === 'verify')!
    expect(verify().attributes('disabled')).toBeDefined()

    await typeUsername(w, 'ada_l')
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
      body: expect.objectContaining({ username: 'ada_l', email: 'ada@example.com', ticket: 'tkt_1' }),
    })
    expect(nav).toHaveBeenCalledWith('/onboarding')
  })

  it('a password containing the username never enables Next', async () => {
    const w = await mountSuspended(Register)
    await reachPasswordStep(w)
    await w.find('#password').setValue('Ada_l-Pass1!')
    await w.find('#confirm').setValue('Ada_l-Pass1!')
    await w.find('#accept-terms').setValue(true)
    const next = w.findAll('button').find(b => b.text().toLowerCase() === 'next')!
    expect(next.attributes('disabled')).toBeDefined()
  })
})
