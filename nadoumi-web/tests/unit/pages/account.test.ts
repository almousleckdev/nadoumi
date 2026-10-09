import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Account from '~/pages/dashboard/account.vue'
import { useToast } from '~/composables/useToast'

const signOut = vi.fn()
const refresh = vi.fn()
const refreshPhoto = vi.fn()
const uploadPhoto = vi.fn()
const photo = vi.hoisted(() => ({ url: '' }))
const toastMessages = () => useToast().toasts.value.map(t => `${t.kind}:${t.message}`)
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({
    user: ref({ userId: 1, username: 'stu_sam', nickName: 'Sam', email: 'sam@example.com' }),
    signOut,
    refresh,
  }),
}))
vi.mock('~/composables/useMyApplicant', () => ({
  useMyApplicant: () => ({ primary: ref({ id: 5, givenName: 'SAM', familyName: 'LEE' }) }),
}))
vi.mock('~/composables/useMyPhoto', () => ({
  useMyPhoto: () => ({ url: computed(() => photo.url), refresh: refreshPhoto }),
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => ({ uploadPhoto }) }))
vi.mock('~/utils/files', async (original) => ({
  ...(await original<typeof import('~/utils/files')>()),
  readAsDataUrl: async () => 'data:image/jpeg;base64,AAAA',
  imageSize: async () => ({ width: 800, height: 800 }),
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
beforeEach(() => {
  fetchImpl.mockClear(); signOut.mockReset(); refresh.mockReset(); refreshPhoto.mockReset(); uploadPhoto.mockReset().mockResolvedValue({ mediaId: 1 })
  photo.url = ''
  useToast().toasts.value = []
})

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
    expect(toastMessages().join(' ').toLowerCase()).toContain('signed out on your other devices')
  })

  it('shows the student by legal name and email, never a blank heading', async () => {
    const w = await mountSuspended(Account)
    expect(w.find('[data-test="account-name"]').text()).toBe('SAM LEE')
    expect(w.text()).toContain('sam@example.com')
    expect(w.text()).toContain('STU-1')
    expect(w.text()).toContain('stu_sam')
  })

  it('shows the profile picture when the student has one, and initials when not', async () => {
    const without = await mountSuspended(Account)
    expect(without.find('[data-test="account-photo"]').exists()).toBe(false)
    expect(without.find('[data-test="account-initial"]').text()).toBe('S')

    photo.url = 'https://cdn.example/me.jpg'
    const withPhoto = await mountSuspended(Account)
    expect(withPhoto.find('[data-test="account-photo"]').attributes('src')).toBe('https://cdn.example/me.jpg')
    expect(withPhoto.find('[data-test="account-initial"]').exists()).toBe(false)
  })

  describe('changing the photo', () => {
    async function pick(w: Awaited<ReturnType<typeof mountSuspended>>, file: File) {
      const input = w.find('[data-test="photo-input"]')
      Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
      await input.trigger('change')
      await flushPromises()
    }
    const big = () => new File([new Uint8Array(1024)], 'me.jpg', { type: 'image/jpeg' })

    it('uploads a valid photo, refreshes the picture and says so with a toast', async () => {
      const w = await mountSuspended(Account)
      await pick(w, big())

      expect(uploadPhoto).toHaveBeenCalledWith(5, expect.any(File))
      expect(refreshPhoto).toHaveBeenCalled()
      expect(toastMessages()).toContain('success:Profile photo updated')
    })

    it('refuses a file that is not an image, without uploading it', async () => {
      const w = await mountSuspended(Account)
      await pick(w, new File(['x'], 'cv.pdf', { type: 'application/pdf' }))

      expect(uploadPhoto).not.toHaveBeenCalled()
      expect(toastMessages().some(m => m.startsWith('error:'))).toBe(true)
    })

    it('reports a failed upload instead of failing silently', async () => {
      uploadPhoto.mockRejectedValue(new Error('down'))
      const w = await mountSuspended(Account)
      await pick(w, big())

      expect(refreshPhoto).not.toHaveBeenCalled()
      expect(toastMessages().some(m => m.startsWith('error:'))).toBe(true)
    })
  })

  it('asks before signing out', async () => {
    useSignOutConfirm().dismiss()
    const w = await mountSuspended(Account)
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(useSignOutConfirm().open.value).toBe(true)
    expect(signOut).not.toHaveBeenCalled()
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
