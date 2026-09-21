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

const { default: student } = await import('~~/server/api/student/[...path]')

beforeEach(() => {
  state.path = ''
  state.search = ''
  state.token = 'jwt-abc'
  state.proxied = undefined
  state.proxyImpl.mockReset()
})

describe('student proxy', () => {
  it('forwards a sub-path request as-is', async () => {
    state.path = 'applicants/1'
    state.proxyImpl.mockResolvedValueOnce({ id: 1 })

    await student({} as never)

    expect(state.proxied).toBe('http://backend/api/student/applicants/1')
  })

  it('does not append a trailing slash for the bare resource — Spring 6+ 404s on one', async () => {
    state.path = ''
    state.proxyImpl.mockResolvedValueOnce([])

    await student({} as never)

    expect(state.proxied).toBe('http://backend/api/student')
  })
})
