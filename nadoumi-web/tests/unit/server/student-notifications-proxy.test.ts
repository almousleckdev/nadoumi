import { describe, it, expect, vi, beforeEach } from 'vitest'

// Same harness as media-proxy.test.ts: the route uses Nitro auto-imports, which the
// Nuxt test env does not transform, so we install them as globals inside vi.hoisted
// (runs before the route module imports).
const state = vi.hoisted(() => {
  const s = {
    path: '',
    search: '',
    token: undefined as string | undefined,
    proxied: undefined as string | undefined,
    proxyImpl: vi.fn(),
  }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.getRouterParam = () => s.path
  g.getRequestURL = () => ({ search: s.search })
  g.backendBaseUrl = () => 'http://backend'
  g.studentToken = () => s.token
  g.proxyRequest = (_e: unknown, target: string, opts: unknown) => {
    s.proxied = target
    return s.proxyImpl(target, opts)
  }
  g.createError = (opts: { statusCode: number, statusMessage?: string }) => {
    const e = new Error(opts.statusMessage ?? 'error') as Error & { statusCode?: number }
    e.statusCode = opts.statusCode
    return e
  }
  return s
})

const { default: notifications } = await import('~~/server/api/student-notifications/[...path]')

async function statusOf(promise: Promise<unknown>): Promise<number> {
  try {
    await promise
  }
  catch (e) {
    return (e as { statusCode?: number }).statusCode ?? 0
  }
  return 0
}

beforeEach(() => {
  state.path = ''
  state.search = ''
  state.token = undefined
  state.proxied = undefined
  state.proxyImpl.mockReset()
})

describe('student-notifications proxy', () => {
  it('forwards an authenticated request to the notifications API with the bearer token', async () => {
    state.token = 'jwt-abc'
    state.path = 'unread-count'
    state.proxyImpl.mockResolvedValueOnce({ count: 3 })

    const res = await notifications({} as never)

    expect(state.proxied).toBe('http://backend/api/notifications/unread-count')
    expect(state.proxyImpl).toHaveBeenCalledWith('http://backend/api/notifications/unread-count', {
      headers: { authorization: 'Bearer jwt-abc' },
    })
    expect(res).toEqual({ count: 3 })
  })

  it('preserves the query string', async () => {
    state.token = 'jwt-abc'
    state.path = ''
    state.search = '?page=2'
    state.proxyImpl.mockResolvedValueOnce([])

    await notifications({} as never)

    expect(state.proxied).toBe('http://backend/api/notifications?page=2')
  })

  it('does not append a trailing slash for the bare list endpoint — Spring 6+ 404s on one', async () => {
    state.token = 'jwt-abc'
    state.path = ''
    state.proxyImpl.mockResolvedValueOnce({ content: [], totalElements: 0 })

    await notifications({} as never)

    expect(state.proxied).toBe('http://backend/api/notifications')
  })

  it('rejects without a signed-in cookie and never proxies', async () => {
    state.token = undefined
    expect(await statusOf(notifications({} as never))).toBe(401)
    expect(state.proxied).toBeUndefined()
  })
})
