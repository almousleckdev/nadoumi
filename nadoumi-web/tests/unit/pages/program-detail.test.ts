import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ProgramDetail from '~/pages/programs/[slug].vue'
import type { ProgramDetail as PDto } from '~/types/catalog'

const mba: PDto = {
  id: 11,
  slug: 'fudan-university-mba',
  universityId: 7,
  universitySlug: 'fudan-university',
  universityName: 'Fudan University',
  universityCity: 'Shanghai',
  universityProvince: 'Shanghai Municipality',
  name: 'MBA',
  nameCn: '工商管理硕士',
  programType: 'DEGREE',
  levels: ['MASTER'],
  field: 'Business',
  teachingLanguage: 'ENGLISH',
  durationMonths: 24,
  tuitionAmount: 38000,
  tuitionCurrency: 'USD',
  summary: 'A two-year MBA taught in English.',
  featured: false,
  hot: true,
  imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/mba.jpg',
  majors: [
    { id: 1, name: 'Finance', nameCn: null },
    { id: 2, name: 'Marketing', nameCn: null },
  ],
  intakes: [
    { id: 1, term: 'AUTUMN_SEPTEMBER', applicationOpen: '2026-03-01', applicationClose: '2026-06-30' },
  ],
  offerings: [
    { id: 1, level: 'MASTER', durationMonths: 24, tuitionAmount: 38000, tuitionCurrency: 'CNY', tuitionPeriod: 'PER_YEAR',
      tuitionAmountUsd: 5300, accommodationAmount: 1500, accommodationCurrency: 'CNY', accommodationPeriod: 'PER_MONTH',
      accommodationAmountUsd: 210, note: 'Shared room' },
    { id: 2, level: 'MASTER', durationMonths: 36, tuitionAmount: 36000, tuitionCurrency: 'CNY', tuitionPeriod: 'PER_YEAR',
      tuitionAmountUsd: 5000, accommodationAmount: null, accommodationCurrency: null, accommodationPeriod: 'PER_MONTH' },
  ],
  admissionRequirements: 'A bachelor degree with good grades.',
  applicationDocuments: 'Old free text document\n',
  minIelts: 6.5,
  applicationFeeAmount: 600, applicationFeeCurrency: 'CNY', applicationFeeAmountUsd: 84,
  scholarshipInfo: 'CSC and university scholarships.',
  careerProspects: 'Consulting and finance.',
}

const requirements = [
  { docType: 'PASSPORT', mandatory: true, notes: 'Photo page', appliesWhen: null },
  { docType: 'STUDY_PLAN', mandatory: false, notes: null, appliesWhen: 'MASTER_OR_DOCTORAL' },
]
const publicGet = vi.fn((path: string) => {
  if (path === 'programs/fudan-university-mba') return Promise.resolve(mba)
  if (path === 'document-requirements/programs/11') return Promise.resolve(requirements)
  return Promise.reject(new Error('404'))
})
mockNuxtImport('useApi', () => () => ({ publicGet, studentFetch: vi.fn() }))

describe('public programme detail page', () => {
  it('renders the programme: name, university link, majors, intakes', async () => {
    const w = await mountSuspended(ProgramDetail, { route: '/programs/fudan-university-mba' })
    await flushPromises()
    const text = w.text()

    expect(text).toContain('MBA')
    expect(text).toContain('工商管理硕士')
    expect(text).toContain('Fudan University')
    expect(text).toContain('Shanghai, Shanghai Municipality') // where the programme is, from its university
    expect(text).toContain('A two-year MBA taught in English.')
    expect(text).toContain('Finance')
    expect(text).toContain('Marketing')
    expect(text).toContain('Autumn (September)')

    // the resolved (absolute) programme image is the hero background, used verbatim — not routed through /media
    const srcs = w.findAll('img').map(i => i.attributes('src'))
    expect(srcs).toEqual(['https://res.cloudinary.com/demo/image/upload/v1/mba.jpg'])
  })

  it('shows every duration of the level with its tuition and accommodation, prices in red', async () => {
    const w = await mountSuspended(ProgramDetail, { route: '/programs/fudan-university-mba' })
    await flushPromises()
    const text = w.text()

    expect(text).toContain('Programme options')
    expect(text).toContain('2 years')
    expect(text).toContain('3 years')
    expect(text).toContain('¥38,000 · $5,300')
    expect(text).toContain('¥1,500 · $210')
    expect(text).toContain('per year')
    expect(text).toContain('per month')
    expect(text).toContain('Shared room')
    const tuition = w.findAll('p').find(el => el.text() === '¥38,000 · $5,300')
    expect(tuition?.classes()).toContain('text-red-600')
    // the facts strip summarises the range and the cheapest prices
    expect(text).toContain('2 years – 3 years')
    expect(text).toContain('¥36,000 · $5,000')
  })

  it('renders the admission content: requirements, language scores, documents, fee, scholarships, career', async () => {
    const w = await mountSuspended(ProgramDetail, { route: '/programs/fudan-university-mba' })
    await flushPromises()
    const text = w.text()

    expect(text).toContain('A bachelor degree with good grades.')
    expect(text).toContain('IELTS 6.5 or above')
    expect(text).toContain('Passport')
    expect(text).toContain('Photo page')
    expect(text).toContain('Study plan')
    expect(text).not.toContain('Old free text document') // the structured list replaces the old free text
    expect(text).toContain('¥600 · $84')
    expect(text).toContain('CSC and university scholarships.')
    expect(text).toContain('Consulting and finance.')
  })

  it('falls back to the old free-text list while a programme has no structured documents', async () => {
    requirements.splice(0, requirements.length)
    const w = await mountSuspended(ProgramDetail, { route: '/programs/fudan-university-mba' })
    await flushPromises()

    expect(w.text()).toContain('Old free text document')
  })

  it('shows a not-available message when the programme is missing', async () => {
    const w = await mountSuspended(ProgramDetail, { route: '/programs/ghost-mba' })
    await flushPromises()
    expect(w.text()).toContain('not available')
  })
})
