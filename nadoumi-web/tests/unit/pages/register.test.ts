import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Register from '~/pages/register.vue'

// `vi.mock('#app', ...)` is inert in this project's nuxt test env, so the Nuxt
// auto-imports the page calls are stubbed with `mockNuxtImport`. `$fetch` is a
// global stub that routes by URL: AuthCaptcha's onMounted hits
// `/api/public/captcha` during mount, so per-call queues are not reliable here.
const { nav } = vi.hoisted(() => ({ nav: vi.fn() }))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

const fetchImpl = vi.fn((url: string, _opts?: Record<string, unknown>) => {
  if (url === '/api/public/captcha') return Promise.resolve({ captchaEnabled: false })
  if (url === '/api/student-account') return Promise.resolve({ signedIn: true })
  if (url === '/api/student-session') {
    return Promise.resolve({
      authenticated: true,
      user: { userId: 1, username: 'sam', nickName: 'Sam' },
      applicants: [],
    })
  }
  return Promise.resolve({})
})
vi.stubGlobal('$fetch', fetchImpl)

beforeEach(() => {
  fetchImpl.mockClear()
  nav.mockReset()
})

describe('register page', () => {
  it('blocks submit when passwords do not match', async () => {
    const w = await mountSuspended(Register)
    await flushPromises()
    await w.find('#fullName').setValue('Sam Lee')
    await w.find('#username').setValue('sam')
    await w.find('#password').setValue('secret1')
    await w.find('#confirm').setValue('secret2')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(fetchImpl.mock.calls.some(c => c[0] === '/api/student-account')).toBe(false)
    expect(w.text()).toContain('Passwords do not match')
  })

  it('posts once to /api/student-account and redirects on success', async () => {
    const w = await mountSuspended(Register)
    await flushPromises()
    await w.find('#fullName').setValue('Sam Lee')
    await w.find('#username').setValue('sam')
    await w.find('#password').setValue('secret1')
    await w.find('#confirm').setValue('secret1')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()
    const accountCalls = fetchImpl.mock.calls.filter(c => c[0] === '/api/student-account')
    expect(accountCalls).toHaveLength(1)
    expect(accountCalls[0]?.[1]).toMatchObject({
      method: 'POST',
      body: expect.objectContaining({ username: 'sam' }),
    })
    expect(nav).toHaveBeenCalledWith('/dashboard/profile')
  })
})
