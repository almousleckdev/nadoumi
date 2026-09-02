import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Account from '~/pages/dashboard/account.vue'

const signOut = vi.fn()
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({
    user: ref({ userId: 1, username: 'stu_sam', nickName: 'Sam', email: 'sam@example.com' }),
    signOut,
  }),
}))

const fetchImpl = vi.fn((_url: string, _opts?: Record<string, unknown>) => Promise.resolve(undefined))
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => { fetchImpl.mockClear(); signOut.mockReset() })

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
})
