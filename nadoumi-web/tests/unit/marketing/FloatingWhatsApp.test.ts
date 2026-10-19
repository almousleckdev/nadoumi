import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import FloatingWhatsApp from '~/components/marketing/FloatingWhatsApp.vue'

describe('FloatingWhatsApp', () => {
  it('opens a wa.me chat with the Nadoumi WhatsApp number in a new tab', async () => {
    const w = await mountSuspended(FloatingWhatsApp)
    const link = w.find('[data-test="floating-whatsapp"]')

    expect(link.attributes('href')).toBe('https://wa.me/8615520576024')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toContain('noopener')
    expect(link.attributes('aria-label')).toBeTruthy()
  })
})
