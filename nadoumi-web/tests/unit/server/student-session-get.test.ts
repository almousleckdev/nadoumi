import { describe, it, expect, vi, beforeEach } from 'vitest'

// The handler relies on Nitro auto-imports (`defineEventHandler`, `$fetch`, and the
// `server/utils/backend` helpers). The Nuxt test environment does not run the
// server-side unimport transform, so we install each name as a global *inside
// `vi.hoisted`* — that block runs before the handler module is imported — then call
// the exported handler directly.
const h3 = vi.hoisted(() => {
  const state = {
    cookie: undefined as string | undefined,
    cleared: false,
    fetchImpl: vi.fn(),
  }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.$fetch = state.fetchImpl
  g.studentToken = () => state.cookie
  g.backendBaseUrl = () => 'http://backend'
  g.clearStudentToken = () => { state.cleared = true }
  return state
})

const { default: handler } = await import('~~/server/api/student-session.get')

beforeEach(() => { h3.cookie = undefined; h3.cleared = false; h3.fetchImpl.mockReset() })

describe('GET /api/student-session', () => {
  it('returns authenticated:false and never calls the API without a cookie', async () => {
    const res = await handler({} as never)
    expect(res).toEqual({ authenticated: false })
    expect(h3.fetchImpl).not.toHaveBeenCalled()
  })

  it('maps /me into a SessionDto when the cookie is valid', async () => {
    h3.cookie = 'jwt'
    h3.fetchImpl.mockResolvedValueOnce({
      userId: 7, username: 'sam', nickName: 'Sam', email: 'sam@example.com',
      accessibleApplicants: [{ applicantId: 3, accessRole: 'OWNER', capabilities: ['VIEW_PROFILE'] }],
    })
    const res = await handler({} as never)
    expect(res).toEqual({
      authenticated: true,
      user: { userId: 7, username: 'sam', nickName: 'Sam', email: 'sam@example.com' },
      applicants: [{ applicantId: 3, accessRole: 'OWNER', capabilities: ['VIEW_PROFILE'] }],
    })
  })

  it('clears the cookie and returns guest on 401', async () => {
    h3.cookie = 'jwt'
    h3.fetchImpl.mockRejectedValueOnce(Object.assign(new Error('x'), { statusCode: 401 }))
    const res = await handler({} as never)
    expect(res).toEqual({ authenticated: false })
    expect(h3.cleared).toBe(true)
  })
})
