import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ScholarshipDetail from '~/pages/scholarships/[slug].vue'
import type { ScholarshipDetail as SDto } from '~/types/catalog'

const dto: SDto = {
  id: 1, slug: 'csc-master', title: 'CSC Master Scholarship',
  summary: 'A fully funded route to a master’s in China.', country: 'CN', city: 'Beijing',
  fundingModel: 'FULLY', hasStipend: true, deadline: '2026-03-31',
  featured: true, recommended: false, hot: false,
  levels: ['MASTER'], categories: ['CSC'],
  intakes: [{ term: 'AUTUMN_SEPTEMBER', applicationClose: '2026-03-31' }],
  benefits: 'Tuition, accommodation, stipend.', requirements: 'Bachelor + IELTS 6.0.', policy: null,
  eligibility: { ageMin: 18, ageMax: 35, gpaMin: 3, ieltsMin: 6, inChina: false, nationalityScope: 'ANY' },
  fees: [{ kind: 'APPLICATION', amountRmb: 710, amountUsd: 100, currency: 'CNY', note: 'Non-refundable' }],
  stipends: [{ level: 'MASTER', amountRmb: 3500, amountUsd: 493, currency: 'CNY', frequency: 'MONTHLY', durationMonths: 36 }],
  accommodation: [{ roomType: 'SINGLE', amountRmb: 1200, amountUsd: 169, currency: 'CNY', note: 'AC, private bathroom' }],
  coverage: [
    { kind: 'TUITION', detail: 'Full waiver' },
    { kind: 'MEDICAL_INSURANCE', detail: null },
  ],
  renewalConditions: 'Renewed annually on a GPA >= 3.0 review.',
  requiresFinancialProof: true, requiresFoundationYear: false,
  documentRequirements: [
    { docType: 'PASSPORT', mandatory: true },
    { docType: 'STUDY_PLAN', mandatory: false, note: '1–2 pages' },
  ],
}

const publicGet = vi.fn((p: string) =>
  p === 'scholarships/csc-master' ? Promise.resolve(dto) : Promise.reject(new Error('404')),
)
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))
mockNuxtImport('useSession', () => () => ({ status: ref('guest') }))

describe('scholarship detail page', () => {
  it('renders the full record: eligibility, fees, stipend and the configured document list', async () => {
    const w = await mountSuspended(ScholarshipDetail, { route: '/scholarships/csc-master' })
    await flushPromises()
    const text = w.text()

    expect(text).toContain('CSC Master Scholarship')
    expect(text).toContain('Fully funded')
    expect(text).toContain('Tuition, accommodation, stipend.')
    expect(text).toContain('Eligibility')
    expect(text).toContain('¥710 · $100')
    expect(text).toContain('¥3,500 · $493')
    expect(text).toContain('Accommodation')
    expect(text).toContain('Single room')
    expect(text).toContain('What the award covers')
    expect(text).toContain('Full waiver')
    expect(text).toContain('Renewal & annual review')
    // document requirements come from config, not hard-coded
    expect(text).toContain('Passport')
    expect(text).toContain('Study plan')
    // guest Apply routes to register
    expect(w.findAll('a').some(a => a.text() === 'Apply now' && a.attributes('href') === '/register')).toBe(true)
  })

  it('shows a not-available state for an unknown slug', async () => {
    const w = await mountSuspended(ScholarshipDetail, { route: '/scholarships/ghost' })
    await flushPromises()
    expect(w.text()).toContain('not available')
  })
})
