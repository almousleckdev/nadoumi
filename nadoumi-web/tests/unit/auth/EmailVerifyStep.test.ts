import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import EmailVerifyStep from '~/components/auth/EmailVerifyStep.vue'

type Wrapper = Awaited<ReturnType<typeof mountSuspended>>

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

async function fillCode(w: Wrapper) {
  const boxes = w.findAll('input').filter((i: { attributes(n: string): string | undefined }) =>
    i.attributes('inputmode') === 'numeric')
  for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
  await flushPromises()
}

describe('EmailVerifyStep', () => {
  it('sends a code, shows the email + Edit control, verifies, then emits verified(ticket)', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_42' })
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
    fetchImpl.mockResolvedValueOnce({ sent: true })
    fetchImpl.mockRejectedValueOnce({ statusCode: 400, data: { detail: 'verification code is invalid or expired' } })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'PASSWORD_RESET' } })
    await w.find('button').trigger('click')
    await flushPromises()
    await fillCode(w)
    expect(w.emitted('verified')).toBeFalsy()
    expect(w.text().toLowerCase()).toContain('invalid or has expired')
    expect(w.text()).toMatch(/4 attempts/i)
  })

  it('Edit email returns to the email stage and emits edit', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await w.find('button').trigger('click')
    await flushPromises()
    const editBtn = w.findAll('button').find(b => b.text().toLowerCase().includes('edit email'))!
    await editBtn.trigger('click')
    expect(w.emitted('edit')).toBeTruthy()
    expect(w.find('#otp-email').exists()).toBe(true)
  })
})
