import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'

// Same harness as student-account-post.test.ts: the handlers use Nitro auto-imports,
// which the Nuxt test env does not transform, so we install them as globals inside
// vi.hoisted (runs before the handler modules import).
const state = vi.hoisted(() => {
  const s = {
    body: {} as Record<string, unknown>,
    cookie: undefined as string | undefined,
    status: 0,
    fetchImpl: vi.fn(),
  }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.$fetch = s.fetchImpl
  g.readBody = () => s.body
  g.backendBaseUrl = () => 'http://backend'
  g.studentToken = () => s.cookie
  return s
})

mockNuxtImport('setResponseStatus', () => (_e: unknown, code: number) => { state.status = code })

const { default: emailOtp } = await import('~~/server/api/student-email-otp.post')
const { default: emailOtpVerify } = await import('~~/server/api/student-email-otp-verify.post')
const { default: passwordReset } = await import('~~/server/api/student-password-reset.post')
const { default: passwordChange } = await import('~~/server/api/student-password.post')

beforeEach(() => {
  state.body = {}
  state.cookie = undefined
  state.status = 0
  state.fetchImpl.mockReset()
})

describe('email-otp + password BFF passthroughs', () => {
  it('student-email-otp forwards the body to the backend email-otp path', async () => {
    state.body = { email: 'a@x.com', purpose: 'REGISTER' }
    state.fetchImpl.mockResolvedValueOnce({ sent: true })
    const res = await emailOtp({} as never)
    expect(state.fetchImpl).toHaveBeenCalledWith('http://backend/api/student/email-otp', expect.objectContaining({
      method: 'POST', body: { email: 'a@x.com', purpose: 'REGISTER' },
    }))
    expect(res).toEqual({ sent: true })
  })

  it('student-email-otp passes an upstream 400 problem+json straight through', async () => {
    state.fetchImpl.mockRejectedValueOnce(Object.assign(new Error('bad'), {
      statusCode: 400, data: { detail: 'captcha failed', status: 400 },
    }))
    const res = await emailOtp({} as never)
    expect(state.status).toBe(400)
    expect(res).toEqual({ detail: 'captcha failed', status: 400 })
  })

  it('student-email-otp-verify forwards to the verify path', async () => {
    state.body = { email: 'a@x.com', purpose: 'REGISTER', otp: '482913' }
    state.fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_1' })
    const res = await emailOtpVerify({} as never)
    expect(state.fetchImpl).toHaveBeenCalledWith('http://backend/api/student/email-otp/verify', expect.objectContaining({ method: 'POST' }))
    expect(res).toEqual({ ticket: 'tkt_1' })
  })

  it('student-password-reset forwards to the reset path', async () => {
    state.body = { ticket: 'tkt_1', newPassword: 'BrandNew1!' }
    state.fetchImpl.mockResolvedValueOnce(undefined)
    await passwordReset({} as never)
    expect(state.fetchImpl).toHaveBeenCalledWith('http://backend/api/student/password/reset', expect.objectContaining({
      method: 'POST', body: { ticket: 'tkt_1', newPassword: 'BrandNew1!' },
    }))
  })

  it('student-password without a cookie is 401 and never calls upstream', async () => {
    state.cookie = undefined
    const res = await passwordChange({} as never)
    expect(state.status).toBe(401)
    expect(res).toEqual({ detail: 'Not signed in' })
    expect(state.fetchImpl).not.toHaveBeenCalled()
  })

  it('student-password attaches the bearer from the cookie', async () => {
    state.cookie = 'jwt-abc'
    state.body = { currentPassword: 'OldPass1!', newPassword: 'NewPass1!' }
    state.fetchImpl.mockResolvedValueOnce(undefined)
    await passwordChange({} as never)
    expect(state.fetchImpl).toHaveBeenCalledWith('http://backend/api/student/password', expect.objectContaining({
      method: 'POST',
      headers: { authorization: 'Bearer jwt-abc' },
    }))
  })
})
