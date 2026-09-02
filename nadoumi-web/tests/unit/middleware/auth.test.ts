import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { useSession } from '~/composables/useSession'
import authMiddleware from '~/middleware/auth'

// `vi.mock('#imports', ...)` is inert in this project's nuxt test env, so the
// Nuxt auto-imports the middleware calls are stubbed with `mockNuxtImport`.
const { nav } = vi.hoisted(() => ({ nav: vi.fn() }))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

const to = { fullPath: '/dashboard/profile' } as never
const from = { fullPath: '/' } as never

beforeEach(() => {
  nav.mockReset()
  useSession().status.value = 'guest'
})

describe('middleware/auth', () => {
  it('guest → redirect object to /login?redirect=...', async () => {
    useSession().status.value = 'guest'
    await authMiddleware(to, from)
    expect(nav).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/dashboard/profile' } })
  })

  it('authed → lets navigation through (no redirect)', async () => {
    useSession().status.value = 'authed'
    await authMiddleware(to, from)
    expect(nav).not.toHaveBeenCalled()
  })
})
