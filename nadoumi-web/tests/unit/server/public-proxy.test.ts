import { describe, it, expect, vi, beforeEach } from 'vitest'

// Same harness as media-proxy.test.ts: the route uses Nitro auto-imports, which the
// Nuxt test env does not transform, so we install them as globals inside vi.hoisted
// (runs before the route module imports).
const state = vi.hoisted(() => {
  const s = {
    path: '',
    search: '',
    proxied: undefined as string | undefined,
    proxyImpl: vi.fn(),
  }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.getRouterParam = () => s.path
  g.getRequestURL = () => ({ search: s.search })
  g.backendBaseUrl = () => 'http://backend'
  g.proxyRequest = (_e: unknown, target: string) => {
    s.proxied = target
    return s.proxyImpl(target)
  }
  return s
})

const { default: publicProxy } = await import('~~/server/api/public/[...path]')

beforeEach(() => {
  state.path = ''
  state.search = ''
  state.proxied = undefined
  state.proxyImpl.mockReset()
})

describe('public proxy', () => {
  it('forwards a sub-path request with its query string', async () => {
    state.path = 'scholarships'
    state.search = '?recommended=true'
    state.proxyImpl.mockResolvedValueOnce({ content: [] })

    await publicProxy({} as never)

    expect(state.proxied).toBe('http://backend/api/public/scholarships?recommended=true')
  })

  it('does not append a trailing slash for the bare resource — Spring 6+ 404s on one', async () => {
    state.path = ''
    state.proxyImpl.mockResolvedValueOnce([])

    await publicProxy({} as never)

    expect(state.proxied).toBe('http://backend/api/public')
  })
})
