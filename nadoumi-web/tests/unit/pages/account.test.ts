import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Account from '~/pages/dashboard/account.vue'

const signOut = vi.fn()
const refresh = vi.fn()
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({
    user: ref({ userId: 1, username: 'stu_sam', nickName: 'Sam', email: 'sam@example.com' }),
    signOut,
    refresh,
  }),
}))
vi.mock('~/composables/useEmailChange', () => ({
  useEmailChange: () => ({
    requestCode: vi.fn(),
    confirm: vi.fn(),
    cooldown: ref(0),
    busy: ref(false),
  }),
}))

const fetchImpl = vi.fn((_url: string, _opts?: Record<string, unknown>) => Promise.resolve(undefined))
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => { fetchImpl.mockClear(); signOut.mockReset(); refresh.mockReset() })

describe('dashboard account page', () => {
  it('changes the password and shows the other-sessions notice', async () => {
    const w = await mountSuspended(Account)
    await w.find('#pw-current').setValue('OldPass1!')
    await w.find('#pw-new').setValue('NewPass1!')
    await w.find('#pw-confirm').setValue('NewPass1!')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(fetchImpl).toHaveBeenCalledWith('/api/student-password', expect.objectContaining({
      method: 'POST',
      body: { currentPassword: 'OldPass1!', newPassword: 'NewPass1!' },
    }))
    expect(w.text().toLowerCase()).toContain('signed out on your other devices')
  })

  it('signs out', async () => {
    const w = await mountSuspended(Account)
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(signOut).toHaveBeenCalled()
  })

  it('links out to the real Profile page, and does not duplicate Notifications/Documents nav with a dead shortcut card', async () => {
    const w = await mountSuspended(Account)

    expect(w.findAll('a').some(a => a.attributes('href')?.includes('/dashboard/profile'))).toBe(true)
    expect(w.text()).not.toContain('Documents & identity')
    expect(w.text()).not.toContain('Notifications')
  })

  it('offers real self-service sign-in email change, not a support mailto', async () => {
    const w = await mountSuspended(Account)

    expect(w.text()).toContain('sam@example.com')
    expect(w.find('#email-new').exists()).toBe(true)
    const mailLinks = w.findAll('a').filter(a => a.attributes('href')?.startsWith('mailto:support@nadoumi.com'))
    // only the danger-zone deletion request remains support-mediated
    expect(mailLinks).toHaveLength(1)
  })

  it('still points account deletion at support — that stays staff-mediated', async () => {
    const w = await mountSuspended(Account)

    const deleteLink = w.findAll('a').find(a => a.attributes('href')?.startsWith('mailto:support@nadoumi.com'))
    expect(deleteLink).toBeTruthy()
    expect(w.text()).toContain('Danger zone')
  })
})
