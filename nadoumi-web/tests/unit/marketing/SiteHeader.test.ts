import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SiteHeader from '~/components/marketing/SiteHeader.vue'

describe('SiteHeader', () => {
  it('shows the wordmark and the primary nav', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.find('.site-brand').text()).toBe('Nadoumi')
    expect(w.text()).toContain('Scholarships')
    expect(w.text()).toContain('Universities')
  })

  it('shows guest auth actions when signed out', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.text()).toContain('Sign in')
    expect(w.text()).toContain('Create profile')
  })
})
