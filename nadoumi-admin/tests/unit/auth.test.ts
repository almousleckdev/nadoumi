import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import Cookies from 'js-cookie'

vi.mock('js-cookie', () => ({
  default: { get: vi.fn(), set: vi.fn(), remove: vi.fn() },
}))

describe('auth token cookie', () => {
  const originalLocation = window.location

  beforeEach(() => {
    vi.resetModules()
    vi.mocked(Cookies.set).mockClear()
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', { value: originalLocation, writable: true })
  })

  it('sets the cookie without `secure` over plain http', async () => {
    Object.defineProperty(window, 'location', { value: { protocol: 'http:' }, writable: true })
    const { setToken } = await import('@/utils/auth')

    setToken('tok')

    expect(Cookies.set).toHaveBeenCalledWith('nadoumi-admin-token', 'tok',
      expect.objectContaining({ secure: false, sameSite: 'lax', expires: 1 }))
  })

  it('sets the cookie with `secure` over https', async () => {
    Object.defineProperty(window, 'location', { value: { protocol: 'https:' }, writable: true })
    const { setToken } = await import('@/utils/auth')

    setToken('tok')

    expect(Cookies.set).toHaveBeenCalledWith('nadoumi-admin-token', 'tok',
      expect.objectContaining({ secure: true }))
  })
})
