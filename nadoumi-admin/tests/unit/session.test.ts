import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const api = vi.hoisted(() => ({ getInfo: vi.fn(), login: vi.fn(), logout: vi.fn() }))
vi.mock('@/api/auth', () => api)

import { useUserStore } from '@/stores/user'
import service from '@/utils/request'
import { CLIENT_HEADERS } from '@/utils/session'

const info = { user: { userName: 'staff', nickName: 'Staff' }, roles: ['staff'], permissions: ['nad:x'] }

describe('admin session (httpOnly cookie)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    api.getInfo.mockReset()
    api.login.mockReset()
    api.logout.mockReset()
  })

  it('restore signs in when the server recognises the cookie', async () => {
    api.getInfo.mockResolvedValue(info)
    const store = useUserStore()

    await store.restore()

    expect(store.signedIn).toBe(true)
    expect(store.loaded).toBe(true)
  })

  it('restore leaves the user signed out when there is no valid session', async () => {
    api.getInfo.mockRejectedValue(new Error('Not authenticated'))
    const store = useUserStore()

    await store.restore()

    expect(store.signedIn).toBe(false)
    expect(store.loaded).toBe(true)
  })

  it('restore asks the server only once per page load', async () => {
    api.getInfo.mockResolvedValue(info)
    const store = useUserStore()

    await store.restore()
    await store.restore()

    expect(api.getInfo).toHaveBeenCalledTimes(1)
  })

  it('login sets the cookie server-side, then loads the profile without holding a token', async () => {
    api.login.mockResolvedValue({ code: 200 })
    api.getInfo.mockResolvedValue(info)
    const store = useUserStore()

    await store.login({ username: 'staff', password: 'pw' })

    expect(api.login).toHaveBeenCalledWith({ username: 'staff', password: 'pw' })
    expect(store.signedIn).toBe(true)
    expect(store).not.toHaveProperty('token')
  })

  it('reset signs the user out', async () => {
    api.getInfo.mockResolvedValue(info)
    const store = useUserStore()
    await store.restore()

    store.reset()

    expect(store.signedIn).toBe(false)
    expect(store.permissions).toEqual([])
  })
})

describe('admin request client', () => {
  it('sends credentials and the client header on every request', () => {
    expect(service.defaults.withCredentials).toBe(true)
    expect(service.defaults.headers).toMatchObject(CLIENT_HEADERS)
  })
})
