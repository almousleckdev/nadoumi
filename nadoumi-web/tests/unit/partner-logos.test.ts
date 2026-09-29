import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PartnerLogos from '~/components/marketing/PartnerLogos.vue'
import type { UniversitySummary } from '~/types/catalog'

const base = { id: 1, slug: 'peking-university', name: 'Peking University', country: 'CN', city: 'Beijing' } as UniversitySummary

describe('PartnerLogos', () => {
  it('links each partner to its university page and shows the logo', async () => {
    const w = await mountSuspended(PartnerLogos, {
      props: { universities: [{ ...base, logoUrl: 'https://res.cloudinary.com/demo/pku.jpg' }] },
    })
    expect(w.find('a').attributes('href')).toContain('/universities/peking-university')
    expect(w.find('img').attributes('src')).toBe('https://res.cloudinary.com/demo/pku.jpg')
  })

  it('falls back to the university name when there is no logo', async () => {
    const w = await mountSuspended(PartnerLogos, { props: { universities: [base] } })
    expect(w.find('img').exists()).toBe(false)
    expect(w.text()).toContain('Peking University')
  })
})
