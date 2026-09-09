import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SiteHeader from '~/components/marketing/SiteHeader.vue'

describe('SiteHeader', () => {
  it('shows the brand logo and the primary nav set (no Programs/Destinations top-level)', async () => {
    const w = await mountSuspended(SiteHeader)
    const logo = w.find('img[alt="Nadoumi"]')
    expect(logo.exists()).toBe(true)
    expect(logo.attributes('src')).toBeTruthy()
    for (const label of ['Home', 'Scholarships', 'Universities', 'About', 'Contact']) {
      expect(w.text()).toContain(label)
    }
    expect(w.text()).not.toContain('Programs')
    expect(w.text()).not.toContain('Destinations')
  })

  it('shows only Sign in when signed out (no Create account, no locale switcher)', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.text()).toContain('Sign in')
    expect(w.text()).not.toContain('Create account')
    // locale switcher removed from the header
    expect(w.findComponent({ name: 'NLocaleSwitcher' }).exists()).toBe(false)
  })
})
