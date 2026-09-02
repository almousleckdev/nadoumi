import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useOtp } from '~/composables/useOtp'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('useOtp', () => {
  it('request posts the email + purpose and starts the cooldown', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })
    const otp = useOtp()
    await otp.request('a@x.com', 'REGISTER')
    expect(fetchImpl).toHaveBeenCalledWith('/api/student-email-otp', expect.objectContaining({
      method: 'POST',
      body: { email: 'a@x.com', purpose: 'REGISTER' },
    }))
    expect(otp.cooldown.value).toBeGreaterThan(0)
  })

  it('sizes the cooldown from retryAfter and reports throttled', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true, throttled: true, retryAfter: 25 })
    const otp = useOtp()
    const res = await otp.request('a@x.com', 'REGISTER')
    expect(res).toEqual({ throttled: true, retryAfter: 25 })
    expect(otp.cooldown.value).toBe(25)
  })

  it('verify returns the ticket string', async () => {
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_9' })
    const otp = useOtp()
    await expect(otp.verify('a@x.com', 'PASSWORD_RESET', '482913')).resolves.toBe('tkt_9')
    expect(fetchImpl).toHaveBeenCalledWith('/api/student-email-otp-verify', expect.objectContaining({
      method: 'POST',
      body: { email: 'a@x.com', purpose: 'PASSWORD_RESET', otp: '482913' },
    }))
  })
})
