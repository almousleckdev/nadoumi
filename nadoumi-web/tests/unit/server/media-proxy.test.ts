import { describe, it, expect, vi, beforeEach } from 'vitest'

// Same harness as otp-and-password-bff.test.ts: the route uses Nitro auto-imports,
// which the Nuxt test env does not transform, so we install them as globals inside
// vi.hoisted (runs before the route module imports).
const state = vi.hoisted(() => {
  const s = {
    path: '',
    proxied: undefined as string | undefined,
    proxyImpl: vi.fn(),
  }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.getRouterParam = () => s.path
  g.backendBaseUrl = () => 'http://backend'
  g.proxyRequest = (_e: unknown, target: string) => {
    s.proxied = target
    return s.proxyImpl(target)
  }
  g.createError = (opts: { statusCode: number; statusMessage?: string }) => {
    const e = new Error(opts.statusMessage ?? 'error') as Error & { statusCode?: number }
    e.statusCode = opts.statusCode
    return e
  }
  return s
})

const { default: media } = await import('~~/server/routes/media/[...path]')

function statusOf(fn: () => unknown): number {
  try {
    fn()
  }
  catch (e) {
    return (e as { statusCode?: number }).statusCode ?? 0
  }
  return 0
}

beforeEach(() => {
  state.path = ''
  state.proxied = undefined
  state.proxyImpl.mockReset()
})

describe('media proxy path guard', () => {
  it('forwards a profile/ path to the backend unchanged', () => {
    state.path = 'profile/upload/2026/x.jpg'
    state.proxyImpl.mockReturnValueOnce('ok')
    const res = media({} as never)
    expect(state.proxied).toBe('http://backend/profile/upload/2026/x.jpg')
    expect(res).toBe('ok')
  })

  it.each([
    'druid/index.html',
    'actuator/health',
    'api/dev/mail/latest',
    'swagger-ui.html',
    '',
  ])('rejects non-profile path %j with 404 and never proxies', (p) => {
    state.path = p
    expect(statusOf(() => media({} as never))).toBe(404)
    expect(state.proxied).toBeUndefined()
  })

  it('rejects a profile-prefixed path containing .. traversal', () => {
    state.path = 'profile/../../etc/passwd'
    expect(statusOf(() => media({} as never))).toBe(404)
    expect(state.proxied).toBeUndefined()
  })
})
