import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ProgramCard from '~/components/marketing/ProgramCard.vue'
import type { ProgramCard as PCard } from '~/types/catalog'

const base: PCard = {
  id: 4,
  slug: 'peking-university-chinese-language-programme',
  universityId: 1,
  universitySlug: 'peking-university',
  universityName: 'Peking University',
  name: 'Chinese Language Programme',
  nameCn: null,
  programType: 'LANGUAGE',
  levels: [],
  field: null,
  teachingLanguage: 'CHINESE',
  durationMonths: 12,
  tuitionAmount: null,
  tuitionCurrency: null,
  summary: null,
  featured: false,
  hot: true,
}

describe('ProgramCard', () => {
  it('renders the resolved (absolute) image URL verbatim', async () => {
    const w = await mountSuspended(ProgramCard, {
      props: { program: { ...base, imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/clp.jpg' } },
    })
    const img = w.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('https://res.cloudinary.com/demo/image/upload/v1/clp.jpg')
  })

  it('renders no <img> element when the programme has no image', async () => {
    const w = await mountSuspended(ProgramCard, {
      props: { program: { ...base, imageUrl: null } },
    })
    expect(w.find('img').exists()).toBe(false)
    // the card still renders its content
    expect(w.text()).toContain('Chinese Language Programme')
  })
})
