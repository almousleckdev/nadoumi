import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import EmailVerifyStep from '~/components/auth/EmailVerifyStep.vue'

type Wrapper = Awaited<ReturnType<typeof mountSuspended>>

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)

// <AuthCaptcha> probes /api/public/captcha on mount; answer that separately so it
// never consumes a queued OTP response. `captchaEnabled` is overridable per test.
let captchaEnabled = false
let otpQueue: Array<{ ok: boolean, value: unknown }> = []

beforeEach(() => {
  captchaEnabled = false
  otpQueue = []
  fetchImpl.mockReset()
  fetchImpl.mockImplementation((url: string) => {
    if (url.includes('/api/public/captcha')) {
      return Promise.resolve({ captchaEnabled, uuid: 'uuid-1', img: 'AAAA' })
    }
    const next = otpQueue.shift()
    if (!next) return Promise.reject(new Error(`no queued response for ${url}`))
    return next.ok ? Promise.resolve(next.value) : Promise.reject(next.value)
  })
})

const resolveNext = (value: unknown) => otpQueue.push({ ok: true, value })
const rejectNext = (value: unknown) => otpQueue.push({ ok: false, value })

async function fillCode(w: Wrapper) {
  const boxes = w.findAll('input').filter((i: { attributes(n: string): string | undefined }) =>
    i.attributes('inputmode') === 'numeric')
  for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
  await flushPromises()
}

describe('EmailVerifyStep', () => {
  it('sends a code, shows the email + Edit control, verifies, then emits verified(ticket)', async () => {
    resolveNext({ sent: true })
    resolveNext({ ticket: 'tkt_42' })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })

    await w.find('button').trigger('click') // [Verify]
    await flushPromises()
    expect(w.text()).toContain('a@x.com')
    expect(w.text().toLowerCase()).toContain('edit email')

    await fillCode(w)
    expect(w.text().toLowerCase()).toContain('email verified')
    await new Promise(r => setTimeout(r, 700))
    expect(w.emitted('verified')?.[0]).toEqual(['tkt_42'])
  })

  it('surfaces a mapped error and decrements the attempts counter on a wrong code', async () => {
    resolveNext({ sent: true })
    rejectNext({ statusCode: 400, data: { detail: 'verification code is invalid or expired' } })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'PASSWORD_RESET' } })
    await w.find('button').trigger('click')
    await flushPromises()
    await fillCode(w)
    expect(w.emitted('verified')).toBeFalsy()
    expect(w.text().toLowerCase()).toContain('invalid or has expired')
    expect(w.text()).toMatch(/4 attempts/i)
  })

  it('shows the "already sent, check spam" note when the backend reports throttled', async () => {
    // e.g. the user reloaded mid-flow (JS cooldown lost) and re-requested within 60s
    resolveNext({ sent: true, throttled: true, retryAfter: 40 })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await w.find('button').trigger('click') // [Verify]
    await flushPromises()
    expect(w.text().toLowerCase()).toContain('check your inbox and spam')
  })

  it('Edit email returns to the email stage and emits edit', async () => {
    resolveNext({ sent: true })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await w.find('button').trigger('click')
    await flushPromises()
    const editBtn = w.findAll('button').find(b => b.text().toLowerCase().includes('edit email'))!
    await editBtn.trigger('click')
    expect(w.emitted('edit')).toBeTruthy()
    expect(w.find('#otp-email').exists()).toBe(true)
  })

  it('treats a framework {code,msg} envelope (missing captcha) as a failed send, not "sent"', async () => {
    resolveNext({ msg: 'CaptchaExpireException: Captcha has expired', code: 500 })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await w.find('button').trigger('click')
    await flushPromises()
    expect(w.emitted('sent')).toBeFalsy()
    expect(w.find('#otp-email').exists()).toBe(true) // still on the email step
    expect(w.text().toLowerCase()).toContain('captcha')
  })

  it('does not call the OTP endpoint while the captcha answer is blank', async () => {
    // Regression: production has the login captcha on; the OTP request must carry
    // it. Sending with no answer only produced a silent failure before.
    captchaEnabled = true
    resolveNext({ sent: true })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await flushPromises()

    await w.find('button').trigger('click')
    await flushPromises()

    const otpCalls = fetchImpl.mock.calls.filter(c => String(c[0]).includes('/api/student-email-otp'))
    expect(otpCalls).toHaveLength(0)
    expect(w.emitted('sent')).toBeFalsy()
    expect(w.find('#otp-email').exists()).toBe(true) // still on the email step
  })
})
