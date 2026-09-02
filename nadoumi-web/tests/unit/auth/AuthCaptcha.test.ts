import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import AuthCaptcha from '~/components/auth/AuthCaptcha.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('AuthCaptcha', () => {
  it('renders nothing and clears uuid when captcha is disabled', async () => {
    fetchImpl.mockResolvedValueOnce({ captchaEnabled: false })
    const w = await mountSuspended(AuthCaptcha, { props: { code: '', uuid: '' } })
    await w.vm.$nextTick()
    expect(w.find('img').exists()).toBe(false)
    expect(w.emitted('update:uuid')?.at(-1)).toEqual([''])
  })

  it('shows the image and wires the uuid when enabled', async () => {
    fetchImpl.mockResolvedValueOnce({ captchaEnabled: true, uuid: 'u1', img: 'AAAA' })
    const w = await mountSuspended(AuthCaptcha, { props: { code: '', uuid: '' } })
    await w.vm.$nextTick()
    expect(w.find('img').attributes('src')).toBe('data:image/jpeg;base64,AAAA')
    expect(w.emitted('update:uuid')?.at(-1)).toEqual(['u1'])
  })
})
