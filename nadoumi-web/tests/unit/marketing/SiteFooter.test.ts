import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SiteFooter from '~/components/marketing/SiteFooter.vue'

describe('SiteFooter', () => {
  it('links the WhatsApp icon to a wa.me chat built from the real contact number', async () => {
    const w = await mountSuspended(SiteFooter)

    const link = w.findAll('a').find(a => a.attributes('href')?.startsWith('https://wa.me/'))
    expect(link).toBeTruthy()
    expect(link!.attributes('href')).toBe('https://wa.me/8615520576024')
    expect(link!.attributes('target')).toBe('_blank')
    expect(link!.attributes('rel')).toContain('noopener')
  })

  it('shows the WeChat QR code without a link — it is scanned, not clicked', async () => {
    const w = await mountSuspended(SiteFooter)

    const qr = w.findAll('img').find(img => img.attributes('src')?.includes('wechat'))
    expect(qr).toBeTruthy()
    expect(qr!.element.closest('a')).toBeNull()
  })

  it('renders clean hover popover cards for both WhatsApp and WeChat with QR codes', async () => {
    const w = await mountSuspended(SiteFooter)

    const waPopover = w.find('[data-test="whatsapp-popover"]')
    expect(waPopover.exists()).toBe(true)
    const waImg = waPopover.find('img')
    expect(waImg.exists()).toBe(true)
    expect(waImg.attributes('src')).toContain('whatsapp')
    expect(waPopover.text()).toContain('WhatsApp')

    const wcPopover = w.find('[data-test="wechat-popover"]')
    expect(wcPopover.exists()).toBe(true)
    const wcImg = wcPopover.find('img')
    expect(wcImg.exists()).toBe(true)
    expect(wcImg.attributes('src')).toContain('wechat')
    expect(wcPopover.text()).toContain('WeChat')
  })

  it('links the Nadoumi social profiles and no longer shows LinkedIn', async () => {
    const w = await mountSuspended(SiteFooter)
    const hrefs = w.findAll('a').map(a => a.attributes('href') ?? '')

    expect(hrefs).toContain('https://www.facebook.com/profile.php?id=61583472043300')
    expect(hrefs.some(h => h.startsWith('https://www.instagram.com/nadoumi_consulting'))).toBe(true)
    expect(hrefs.some(h => h.startsWith('https://www.tiktok.com/@nadoumi.consultin'))).toBe(true)
    expect(hrefs.some(h => h.includes('linkedin.com'))).toBe(false)
  })
})
