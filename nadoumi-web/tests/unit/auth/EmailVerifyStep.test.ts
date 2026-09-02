import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import EmailVerifyStep from '~/components/auth/EmailVerifyStep.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('EmailVerifyStep', () => {
  it('sends a code then emits verified(ticket) once the OTP resolves', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })          // request
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_42' })    // verify

    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await w.find('button').trigger('click')                  // [Verify]
    await new Promise(r => setTimeout(r))

    const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
    await new Promise(r => setTimeout(r))

    expect(w.emitted('verified')?.[0]).toEqual(['tkt_42'])
  })

  it('surfaces a problem message when the code is wrong', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })
    fetchImpl.mockRejectedValueOnce({ data: { detail: 'verification code is invalid or expired' } })

    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'PASSWORD_RESET' } })
    await w.find('button').trigger('click')
    await new Promise(r => setTimeout(r))
    const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue('9')
    await new Promise(r => setTimeout(r))

    expect(w.emitted('verified')).toBeFalsy()
    expect(w.text().toLowerCase()).toContain('invalid or expired')
  })
})
