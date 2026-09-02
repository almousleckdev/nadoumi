import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'

// The handler relies on Nitro auto-imports (`defineEventHandler`, `readBody`,
// `$fetch`, and the `server/utils/backend` helpers). The Nuxt test environment does
// not run the server-side unimport transform, so we install those names as globals
// *inside `vi.hoisted`* — that block runs before the handler module is imported.
// `setResponseStatus` is also a Nuxt *app* auto-import, so the test transform
// rewrites it to a real import; it is stubbed with `mockNuxtImport` instead.
const state = vi.hoisted(() => {
  const s = {
    body: {} as Record<string, unknown>,
    token: undefined as string | undefined,
    status: 0,
    fetchImpl: vi.fn(),
  }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.$fetch = s.fetchImpl
  g.readBody = () => s.body
  g.backendBaseUrl = () => 'http://backend'
  g.setStudentToken = (_e: unknown, v: string) => { s.token = v }
  return s
})

mockNuxtImport('setResponseStatus', () => (_e: unknown, code: number) => { state.status = code })

const { default: handler } = await import('~~/server/api/student-account.post')

beforeEach(() => {
  state.body = { username: 'sam', password: 'secret1', fullName: 'Sam Lee', email: 's@x.io' }
  state.token = undefined; state.status = 0; state.fetchImpl.mockReset()
})

describe('POST /api/student-account', () => {
  it('registers then logs in server-side and sets the cookie; body carries no token', async () => {
    state.fetchImpl
      .mockResolvedValueOnce({ userId: 1, username: 'sam' })   // register
      .mockResolvedValueOnce({ token: 'JWT123' })              // login
    const res = await handler({} as never)
    expect(state.fetchImpl).toHaveBeenNthCalledWith(1, 'http://backend/api/student/register', expect.objectContaining({ method: 'POST' }))
    expect(state.fetchImpl).toHaveBeenNthCalledWith(2, 'http://backend/api/student/login', expect.objectContaining({ method: 'POST' }))
    expect(state.token).toBe('JWT123')
    expect(res).toEqual({ signedIn: true })
    expect(JSON.stringify(res)).not.toContain('JWT123')
  })

  it('passes the register problem+json + status through and sets no cookie', async () => {
    state.fetchImpl.mockRejectedValueOnce(Object.assign(new Error('dup'), {
      statusCode: 409, data: { title: 'Conflict', detail: 'username taken', status: 409 },
    }))
    const res = await handler({} as never)
    expect(state.status).toBe(409)
    expect(res).toEqual({ title: 'Conflict', detail: 'username taken', status: 409 })
    expect(state.token).toBeUndefined()
  })
})
