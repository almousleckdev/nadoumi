import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DiscoverySection from '~/components/marketing/DiscoverySection.vue'

const base = { title: 'Featured universities', carouselLabel: 'Featured universities' }

describe('DiscoverySection', () => {
  it('shows a skeleton row while pending and hides the arrows', async () => {
    const w = await mountSuspended(DiscoverySection, {
      props: { ...base, pending: true },
      slots: { default: () => 'CARD' },
    })
    expect(w.find('.animate-pulse').exists()).toBe(true)
    expect(w.findComponent({ name: 'CarouselArrows' }).exists()).toBe(false)
    expect(w.text()).not.toContain('CARD')
  })

  it('shows the error state', async () => {
    const w = await mountSuspended(DiscoverySection, {
      props: { ...base, error: 'boom' },
    })
    expect(w.text()).toContain('boom')
  })

  it('shows the empty text (never fake items) when empty', async () => {
    const w = await mountSuspended(DiscoverySection, {
      props: { ...base, empty: true, emptyText: 'Nothing published yet.' },
      slots: { default: () => 'CARD' },
    })
    expect(w.text()).toContain('Nothing published yet.')
    expect(w.text()).not.toContain('CARD')
  })

  it('renders items + a View all link when loaded', async () => {
    const w = await mountSuspended(DiscoverySection, {
      props: { ...base, viewAllTo: '/universities', viewAllLabel: 'View all universities' },
      slots: { default: () => 'CARD' },
    })
    expect(w.text()).toContain('CARD')
    expect(w.text()).toContain('View all universities')
    expect(w.find('a[href="/universities"]').exists()).toBe(true)
  })
})
