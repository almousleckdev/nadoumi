import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SiteFooter from '~/components/marketing/SiteFooter.vue'

describe('SiteFooter', () => {
  it('links the WhatsApp icon to a wa.me chat built from the real contact number', async () => {
    const w = await mountSuspended(SiteFooter)

    const link = w.findAll('a').find(a => a.attributes('href')?.startsWith('https://wa.me/'))
    expect(link).toBeTruthy()
    expect(link!.attributes('href')).toBe('https://wa.me/8615908237607')
    expect(link!.attributes('target')).toBe('_blank')
    expect(link!.attributes('rel')).toContain('noopener')
  })

  it('shows the WeChat QR code without a link — it is scanned, not clicked', async () => {
    const w = await mountSuspended(SiteFooter)

    const qr = w.findAll('img').find(img => img.attributes('src')?.includes('wechat'))
    expect(qr).toBeTruthy()
    expect(qr!.element.closest('a')).toBeNull()
  })

  it('shows both QR codes at a legible size, not a tiny illegible icon', async () => {
    const w = await mountSuspended(SiteFooter)

    const whatsappImg = w.findAll('img').find(img => img.attributes('src')?.includes('whatsapp'))
    const wechatImg = w.findAll('img').find(img => img.attributes('src')?.includes('wechat'))
    // both are real QR codes (not logo icons) — anything much smaller than this is unscannable noise
    expect(whatsappImg!.classes()).toContain('h-28')
    expect(wechatImg!.classes()).toContain('h-28')
    expect(w.text()).toContain('WhatsApp')
    expect(w.text()).toContain('WeChat')
  })
})
