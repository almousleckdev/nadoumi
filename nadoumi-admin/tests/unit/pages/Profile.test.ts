import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  updatePassword: vi.fn(),
  uploadAvatar: vi.fn(),
  getSession: vi.fn(),
}))
vi.mock('@/api/profile', () => api)

const message = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, success: message.success, error: message.error } }
})

import Profile from '@/views/profile.vue'
import { useUserStore } from '@/stores/user'

const profile = {
  data: { userName: 'admin', nickName: 'Admin', email: 'admin@example.com', phonenumber: '', sex: '0', deptName: 'HQ', avatar: '' },
  roleGroup: 'Super Admin',
  postGroup: 'CEO',
}
const session = {
  loginTime: 1790000000000, loggedInForSeconds: 4500, expiresInSeconds: 90000, ipaddr: '10.0.0.7',
  location: 'Beijing', browser: 'Chrome', os: 'macOS',
}

async function mountProfile() {
  const w = mount(Profile, mountOpts())
  await flushPromises()
  return w
}

const buttonByText = (w: ReturnType<typeof mount>, text: string) => w.findAll('button').filter(b => b.text() === text)

describe('Profile', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    message.success.mockReset()
    message.error.mockReset()
    api.getProfile.mockResolvedValue(profile)
    api.getSession.mockResolvedValue(session)
    api.updateProfile.mockResolvedValue(undefined)
    api.updatePassword.mockResolvedValue(undefined)
    api.uploadAvatar.mockResolvedValue({ imgUrl: 'https://cdn.example/a.png' })
  })

  it('shows the account, its roles and the current session', async () => {
    const w = await mountProfile()
    expect(w.text()).toContain('admin')
    expect(w.text()).toContain('Super Admin')
    expect(w.text()).toContain('CEO')
    expect(w.text()).toContain('10.0.0.7')
    expect(w.text()).toContain('Chrome · macOS')
    expect(w.text()).toContain('1h 15m')
    expect(w.text()).toContain('1d 1h')
  })

  it('still shows the account when the session detail cannot be loaded', async () => {
    api.getSession.mockRejectedValue(new Error('no session'))
    const w = await mountProfile()
    expect(w.text()).toContain('Super Admin')
    expect(w.text()).not.toContain('10.0.0.7')
  })

  it('warns only while the initial password must still be changed', async () => {
    expect((await mountProfile()).find('.el-alert').exists()).toBe(false)
    useUserStore().mustChangePassword = true
    expect((await mountProfile()).find('.el-alert').exists()).toBe(true)
  })

  it('fills the form from the profile, saves it and updates the shown name', async () => {
    const w = await mountProfile()
    const inputs = w.findAll('input.el-input__inner')
    expect((inputs[0]!.element as HTMLInputElement).value).toBe('Admin')
    await inputs[0]!.setValue('Boss')
    await buttonByText(w, 'Save')[0]!.trigger('click')
    await flushPromises()

    expect(api.updateProfile).toHaveBeenCalledWith({ nickName: 'Boss', phonenumber: '', email: 'admin@example.com', sex: '0' })
    expect(useUserStore().nickName).toBe('Boss')
    expect(message.success).toHaveBeenCalled()
  })

  it('requires a nickname and a well-formed email and phone number', async () => {
    const w = await mountProfile()
    const rules = w.findAllComponents({ name: 'ElForm' })[0]!.props('rules') as Record<string, Array<Record<string, unknown>>>
    expect(rules.nickName!.some(r => r.required === true)).toBe(true)
    expect(rules.email!.some(r => r.type === 'email')).toBe(true)
    const pattern = rules.phonenumber!.find(r => r.pattern)!.pattern as RegExp
    expect(pattern.test('')).toBe(true)
    expect(pattern.test('13800138000')).toBe(true)
    expect(pattern.test('abc')).toBe(false)
  })

  it('rejects a confirmation that differs from the new password', async () => {
    const w = await mountProfile()
    const pw = w.findAll('input[type="password"]')
    await pw[1]!.setValue('NewPass1!')
    const rules = w.findAllComponents({ name: 'ElForm' })[1]!.props('rules') as Record<string, Array<{ validator?: (r: unknown, v: string, cb: (e?: Error) => void) => void }>>
    const validator = rules.confirmPassword![0]!.validator!

    const mismatch = vi.fn()
    validator(null, 'Different1!', mismatch)
    expect(mismatch.mock.calls[0]![0]).toBeInstanceOf(Error)

    const match = vi.fn()
    validator(null, 'NewPass1!', match)
    expect(match).toHaveBeenCalledWith()
    expect(api.updatePassword).not.toHaveBeenCalled()
  })

  it('changes the password, clears the fields and lifts the initial-password warning', async () => {
    useUserStore().mustChangePassword = true
    const w = await mountProfile()
    const pw = w.findAll('input[type="password"]')
    await pw[0]!.setValue('OldPass1!')
    await pw[1]!.setValue('NewPass1!')
    await pw[2]!.setValue('NewPass1!')
    await buttonByText(w, 'Save')[1]!.trigger('click')
    await flushPromises()

    expect(api.updatePassword).toHaveBeenCalledWith('OldPass1!', 'NewPass1!')
    expect(useUserStore().mustChangePassword).toBe(false)
    expect((w.findAll('input[type="password"]')[0]!.element as HTMLInputElement).value).toBe('')
  })

  it('uploads a valid avatar and shows it', async () => {
    const w = await mountProfile()
    const upload = w.findComponent({ name: 'ElUpload' })
    const file = new File(['x'], 'me.png', { type: 'image/png' })
    const before = upload.props('beforeUpload') as (f: File) => boolean
    expect(before(file)).toBe(false)
    await flushPromises()

    expect(api.uploadAvatar).toHaveBeenCalledWith(file)
    expect(useUserStore().avatar).toBe('https://cdn.example/a.png')
    expect(w.find('img.profile__avatar').attributes('src')).toContain('a.png')
  })

  it('rejects an avatar that is not an image or is too large', async () => {
    const w = await mountProfile()
    const before = w.findComponent({ name: 'ElUpload' }).props('beforeUpload') as (f: File) => boolean
    expect(before(new File(['x'], 'cv.pdf', { type: 'application/pdf' }))).toBe(false)
    const big = new File(['x'], 'big.png', { type: 'image/png' })
    Object.defineProperty(big, 'size', { value: 3 * 1024 * 1024 })
    expect(before(big)).toBe(false)
    expect(api.uploadAvatar).not.toHaveBeenCalled()
    expect(message.error).toHaveBeenCalledTimes(2)
  })
})
