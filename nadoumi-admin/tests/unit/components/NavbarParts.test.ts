import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const push = vi.hoisted(() => vi.fn())
vi.mock('vue-router', async (orig) => ({ ...(await orig<typeof import('vue-router')>()), useRouter: () => ({ push }) }))

const confirmMock = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: confirmMock }) }))

const setLocale = vi.hoisted(() => vi.fn())
vi.mock('@/lang', () => ({ setLocale }))

import UserMenu from '@/layout/components/UserMenu.vue'
import LocaleToggle from '@/layout/components/LocaleToggle.vue'
import { useUserStore } from '@/stores/user'

describe('UserMenu', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockReset()
    confirmMock.mockReset()
  })

  it('shows the initial of the nickname when there is no avatar', () => {
    const store = useUserStore()
    store.nickName = 'amina'
    const w = mount(UserMenu, mountOpts())
    expect(w.find('.topbar__avatar').text()).toBe('A')
    expect(w.find('img').exists()).toBe(false)
  })

  it('falls back to a question mark with no name at all', () => {
    const w = mount(UserMenu, mountOpts())
    expect(w.find('.topbar__avatar').text()).toBe('?')
  })

  it('stays signed in when the sign-out confirmation is declined', async () => {
    confirmMock.mockResolvedValue(false)
    const store = useUserStore()
    const logout = vi.spyOn(store, 'logout').mockResolvedValue(undefined as never)
    const w = mount(UserMenu, mountOpts())
    await (w.vm as unknown as { onCommand: (c: string) => Promise<void> }).onCommand('logout')
    await flushPromises()
    expect(logout).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('signs out and returns to the login page after confirmation', async () => {
    confirmMock.mockResolvedValue(true)
    const store = useUserStore()
    const logout = vi.spyOn(store, 'logout').mockResolvedValue(undefined as never)
    const w = mount(UserMenu, mountOpts())
    await (w.vm as unknown as { onCommand: (c: string) => Promise<void> }).onCommand('logout')
    await flushPromises()
    expect(logout).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('opens the profile page from the menu', async () => {
    const w = mount(UserMenu, mountOpts())
    await (w.vm as unknown as { onCommand: (c: string) => Promise<void> }).onCommand('profile')
    expect(push).toHaveBeenCalledWith('/profile')
  })
})

describe('LocaleToggle', () => {
  beforeEach(() => setLocale.mockReset())

  it('offers Chinese while the console is in English', async () => {
    const w = mount(LocaleToggle, mountOpts())
    expect(w.text()).toBe('中文')
    await w.find('button').trigger('click')
    expect(setLocale).toHaveBeenCalledWith('zh')
  })
})
