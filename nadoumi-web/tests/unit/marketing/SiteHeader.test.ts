import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SiteHeader from '~/components/marketing/SiteHeader.vue'

describe('SiteHeader', () => {
  it('shows the wordmark and the primary nav set (no Programs/Destinations top-level)', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.find('.site-brand').text()).toBe('Nadoumi')
    for (const label of ['Home', 'Scholarships', 'Universities', 'About', 'Contact']) {
      expect(w.text()).toContain(label)
    }
    expect(w.text()).not.toContain('Programs')
    expect(w.text()).not.toContain('Destinations')
  })

  it('shows Sign in + Create account when signed out', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.text()).toContain('Sign in')
    expect(w.text()).toContain('Create account')
  })
})
