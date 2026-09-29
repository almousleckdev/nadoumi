import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  getScholarship: vi.fn(),
  getScholarshipInternal: vi.fn(),
  putScholarshipInternal: vi.fn(),
}))
vi.mock('@/api/scholarship', async orig => ({ ...(await orig<typeof import('@/api/scholarship')>()), ...api }))

const programApi = vi.hoisted(() => ({ listPrograms: vi.fn() }))
vi.mock('@/api/program', async orig => ({ ...(await orig<typeof import('@/api/program')>()), ...programApi }))

import ScholarshipDetail from '@/views/scholarships/detail.vue'
import { useUserStore } from '@/stores/user'

const scholarship = {
  view: {
    id: 7, slug: 'csc-master', referenceCode: 'NAD-SCH-0007', title: 'CSC Master Scholarship', country: 'CN',
    city: 'Beijing', province: null, fundingModel: 'FULLY', field: 'Engineering', teachingLanguage: 'ENGLISH',
    deadline: '2026-04-01', levels: ['MASTER'], categories: ['CSC'], summary: 'Fully funded award.',
    requiresFinancialProof: true, requiresFoundationYear: false, slots: 20,
    heroUrl: 'https://res.cloudinary.com/hero.png', coverUrl: null,
    eligibility: { ageMax: 35, nationalityScope: 'INCLUDE', acceptedCountries: 'MA, DZ', gpaMin: 3, notes: 'Bring transcripts.' },
    applicationFee: { amountRmb: 800, amountUsd: 110, currency: 'CNY' }, serviceFee: null,
    fees: [{ kind: 'REGISTRATION', amountRmb: 400, amountUsd: 55, currency: 'CNY', note: null }],
    stipends: [{ level: 'MASTER', amountRmb: 3000, amountUsd: 414, currency: 'CNY', frequency: 'MONTHLY', durationMonths: 24, conditions: 'Full time' }],
    accommodation: [{ roomType: 'DOUBLE', amountRmb: null, amountUsd: null, currency: 'CNY', note: 'On campus' }],
    coverage: [{ kind: 'TUITION', detail: 'All years' }],
    renewalConditions: 'Pass every exam.',
    intakes: [{ term: 'AUTUMN_SEPTEMBER', applicationOpen: null, applicationClose: '2026-04-01' }],
    documentRequirements: [{ docType: 'PASSPORT', mandatory: true, note: null }],
    benefits: 'Tuition and stipend.', requirements: null, policy: null,
  },
  status: 'ACTIVE', publishStatus: 'PUBLISHED',
}

const internal = {
  universityId: 3, universityName: 'Tsinghua University', programId: 11, internalStatus: 'NEGOTIATING',
  operationalNotes: 'Contact the dean.', confidentialTerms: 'Fifteen percent commission.', commissionModelJson: null,
}

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/scholarships/:id', component: { template: '<div />' } }],
})

async function mountDetail() {
  await router.push('/scholarships/7')
  const w = mount(ScholarshipDetail, {
    global: {
      ...mountOpts().global,
      plugins: [...mountOpts().global.plugins, router],
      stubs: { ScholarshipDrawer: true },
    },
  })
  await flushPromises()
  return w
}

describe('Scholarship detail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    api.getScholarship.mockReset().mockResolvedValue(scholarship)
    api.getScholarshipInternal.mockReset().mockResolvedValue(internal)
    api.putScholarshipInternal.mockReset().mockResolvedValue(internal)
    programApi.listPrograms.mockReset().mockResolvedValue({ content: [{ id: 11, name: 'Mechanical Engineering' }] })
  })

  it('loads the scholarship by route id and renders its identity', async () => {
    const w = await mountDetail()
    expect(api.getScholarship).toHaveBeenCalledWith('7')
    expect(w.text()).toContain('CSC Master Scholarship')
    expect(w.text()).toContain('NAD-SCH-0007')
    expect(w.text()).toContain('Engineering')
    expect(w.text()).toContain('Fully funded award.')
  })

  it('renders eligibility criteria including the nationality scope', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('≥ 3')
    expect(w.text()).toContain('Only these countries: MA, DZ')
    expect(w.text()).toContain('Bring transcripts.')
  })

  it('renders fees with both currencies', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('¥800 · $110')
    expect(w.text()).toContain('¥400 · $55')
  })

  it('renders the stipend, coverage, intakes, documents and prose sections', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('¥3,000 · $414')
    expect(w.text()).toContain('All years')
    expect(w.text()).toContain('Pass every exam.')
    expect(w.text()).toContain('Tuition and stipend.')
  })

  it('hides optional sections when the scholarship has no such data', async () => {
    api.getScholarship.mockResolvedValue({
      ...scholarship,
      view: {
        ...scholarship.view, eligibility: null, applicationFee: null, fees: [], stipends: [],
        accommodation: [], coverage: [], renewalConditions: null, intakes: [], documentRequirements: [],
        benefits: null,
      },
    })
    const w = await mountDetail()
    expect(w.text()).not.toContain('All years')
    expect(w.text()).not.toContain('Bring transcripts.')
    expect(w.text()).not.toContain('¥3,000')
  })

  it('shows the confidential linkage to a staff member who may view it', async () => {
    const w = await mountDetail()
    expect(api.getScholarshipInternal).toHaveBeenCalledWith('7')
    expect(w.text()).toContain('Confidential linkage')
    expect(w.text()).toContain('Tsinghua University')
    expect(w.text()).toContain('Fifteen percent commission.')
    expect(w.text()).toContain('Mechanical Engineering')
  })

  it('never requests or renders the confidential linkage without the internal view permission', async () => {
    useUserStore().permissions = ['nad:scholarship:list', 'nad:scholarship:edit']
    const w = await mountDetail()
    expect(api.getScholarshipInternal).not.toHaveBeenCalled()
    expect(w.text()).not.toContain('Confidential linkage')
    expect(w.text()).not.toContain('Tsinghua University')
    expect(w.text()).not.toContain('Fifteen percent commission.')
  })

  it('hides the edit linkage control without the internal edit permission', async () => {
    useUserStore().permissions = ['nad:scholarship:internal:view']
    const w = await mountDetail()
    expect(w.text()).toContain('Tsinghua University')
    expect(w.findAll('button').map(b => b.text())).not.toContain('Edit linkage')
  })

  it('saves the linkage and clears the linked programme when no university is set', async () => {
    api.getScholarshipInternal.mockResolvedValue({ ...internal, universityId: null, universityName: null, programId: 11 })
    const w = await mountDetail()
    await w.findAll('button').find(b => b.text() === 'Edit linkage')!.trigger('click')
    await flushPromises()
    const save = w.findAll('button').filter(b => b.text() === 'Save').at(-1)!
    await save.trigger('click')
    await flushPromises()
    expect(api.putScholarshipInternal).toHaveBeenCalledWith('7', expect.objectContaining({
      universityId: null, programId: null, internalStatus: 'NEGOTIATING',
    }))
  })

  it('shows an error state when the scholarship cannot be loaded', async () => {
    api.getScholarship.mockRejectedValue(new Error('boom'))
    const w = await mountDetail()
    expect(w.text()).toContain('boom')
    expect(w.text()).not.toContain('CSC Master Scholarship')
  })
})
