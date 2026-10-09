import { describe, it, expect } from 'vitest'
import { blankForm, buildPayload, formFromScholarship } from '@/views/scholarships/scholarshipForm'
import type { Scholarship } from '@/api/scholarship'

const scholarship = {
  view: {
    id: 7, slug: 'csc-master', title: 'CSC Master', country: 'CN', fundingModel: 'FULLY',
    hasStipend: true, featured: true, recommended: false, hot: true,
    requiresFinancialProof: true, requiresFoundationYear: false,
    heroMediaId: 201, coverMediaId: 202,
    levels: ['MASTER', 'NON_DEGREE'], categories: ['CSC'],
    nonDegreeDuration: 'ONE_YEAR', applicationChannel: 'CSC_AGENCY', agencyNumber: 'A-12',
    intakes: [{ term: 'AUTUMN_SEPTEMBER', applicationOpen: '2026-01-01', applicationClose: '2026-04-01' }],
    fees: [{ kind: 'REGISTRATION', amountRmb: 400, amountUsd: 55, currency: 'CNY', note: null }],
    stipends: [{ level: 'MASTER', amountRmb: 3000, amountUsd: 414, currency: 'CNY', frequency: 'MONTHLY', durationMonths: 24, conditions: null }],
    accommodation: [{ roomType: 'DOUBLE', amountRmb: null, amountUsd: null, currency: 'CNY', note: 'on campus' }],
    coverage: [{ kind: 'TUITION', detail: null }],
    documentRequirements: [{ docType: 'PASSPORT', mandatory: true, note: null }],
    eligibility: { ageMax: 35, nationalityScope: 'EXCLUDE', acceptedCountries: 'CN', gpaMin: 3, notes: null },
    applicationFee: { amountRmb: 800, amountUsd: 110, currency: 'CNY' },
    serviceFee: null, slots: 20, deadline: '2026-04-01',
  },
  status: 'ACTIVE', publishStatus: 'PUBLISHED', remark: null,
} as unknown as Scholarship

describe('blankForm', () => {
  it('starts as an active draft that is fully funded with no levels', () => {
    const form = blankForm()
    expect(form).toMatchObject({
      country: '', fundingModel: 'FULLY', status: 'ACTIVE', publishStatus: 'DRAFT', levels: [],
    })
    expect(form.eligibility.nationalityScope).toBe('ANY')
  })

  it('returns independent objects on every call', () => {
    const a = blankForm()
    a.levels.push('MASTER')
    expect(blankForm().levels).toEqual([])
  })
})

describe('buildPayload', () => {
  it('trims text, uppercases the country and turns blanks into null', () => {
    const form = blankForm()
    Object.assign(form, { title: '  CSC  ', country: ' cn ', summary: '   ', city: ' Beijing ' })
    const p = buildPayload(form)
    expect(p).toMatchObject({ title: 'CSC', country: 'CN', summary: null, city: 'Beijing', province: null })
  })

  it('keeps the agency number only for the CSC agency channel', () => {
    const form = blankForm()
    Object.assign(form, { agencyNumber: 'A-1', applicationChannel: 'DIRECT' })
    expect(buildPayload(form).agencyNumber).toBeNull()
    form.applicationChannel = 'CSC_AGENCY'
    expect(buildPayload(form).agencyNumber).toBe('A-1')
  })

  it('keeps the non-degree duration only when NON_DEGREE is a selected level', () => {
    const form = blankForm()
    Object.assign(form, { nonDegreeDuration: 'ONE_YEAR', levels: ['MASTER'] })
    expect(buildPayload(form).nonDegreeDuration).toBeNull()
    form.levels = ['NON_DEGREE']
    expect(buildPayload(form).nonDegreeDuration).toBe('ONE_YEAR')
  })

  it('omits eligibility until at least one criterion is set', () => {
    const form = blankForm()
    expect(buildPayload(form).eligibility).toBeNull()
    form.eligibility.ageMin = 18
    expect(buildPayload(form).eligibility).toMatchObject({ ageMin: 18, nationalityScope: 'ANY' })
  })

  it('drops accepted countries when the nationality scope is ANY', () => {
    const form = blankForm()
    form.eligibility.ageMin = 18
    form.eligibility.acceptedCountries = 'CN'
    expect(buildPayload(form).eligibility?.acceptedCountries).toBeNull()
    form.eligibility.nationalityScope = 'INCLUDE'
    expect(buildPayload(form).eligibility?.acceptedCountries).toBe('CN')
  })

  it('drops incomplete fee, stipend, intake, coverage and document rows', () => {
    const form = blankForm()
    form.fees.push({ kind: 'REGISTRATION', amount: null, currency: 'CNY', note: '' })
    form.fees.push({ kind: 'REGISTRATION', amount: 400, currency: 'USD', note: ' x ' })
    form.intakes.push({ term: '', applicationOpen: null, applicationClose: null })
    form.documentRequirements.push({ docType: '  ', mandatory: true, note: '' })
    form.documentRequirements.push({ docType: ' passport ', mandatory: true, note: '' })
    const p = buildPayload(form)
    expect(p.fees).toEqual([{ kind: 'REGISTRATION', amount: 400, currency: 'CNY', note: 'x' }])
    expect(p.intakes).toEqual([])
    expect(p.documentRequirements).toEqual([{ docType: 'PASSPORT', mandatory: true, note: null }])
  })

  it('flags a stipend only when a stipend amount is present', () => {
    const form = blankForm()
    form.levelStipends.push({ level: 'MASTER', amount: null, currency: 'CNY', frequency: 'MONTHLY', durationMonths: null, conditions: '' })
    expect(buildPayload(form).hasStipend).toBe(false)
    form.levelStipends[0]!.amount = 3000
    expect(buildPayload(form).hasStipend).toBe(true)
  })

  it('sets a fee currency only when the fee amount is present', () => {
    const form = blankForm()
    expect(buildPayload(form)).toMatchObject({ applicationFeeAmount: null, applicationFeeCurrency: null })
    form.applicationFeeAmount = 800
    expect(buildPayload(form)).toMatchObject({ applicationFeeAmount: 800, applicationFeeCurrency: 'CNY' })
  })
})

describe('formFromScholarship', () => {
  it('copies the aggregate into the form and turns missing text into empty strings', () => {
    const form = formFromScholarship(scholarship)
    expect(form).toMatchObject({
      id: 7, title: 'CSC Master', country: 'CN', summary: '', levels: ['MASTER', 'NON_DEGREE'],
      categoryCodes: ['CSC'], heroMediaId: 201, coverMediaId: 202, hot: true, publishStatus: 'PUBLISHED',
      applicationFeeAmount: 800, serviceFeeAmount: null, slots: 20, deadline: '2026-04-01',
    })
    expect(form.fees).toEqual([{ kind: 'REGISTRATION', amount: 400, currency: 'CNY', note: '' }])
    expect(form.eligibility).toMatchObject({ ageMax: 35, nationalityScope: 'EXCLUDE', acceptedCountries: 'CN', notes: '' })
  })

  it('survives a round trip through buildPayload', () => {
    const payload = buildPayload(formFromScholarship(scholarship))
    expect(payload).toMatchObject({
      title: 'CSC Master', country: 'CN', levels: ['MASTER', 'NON_DEGREE'], categoryCodes: ['CSC'],
      nonDegreeDuration: 'ONE_YEAR', agencyNumber: 'A-12', hasStipend: true,
      applicationFeeAmount: 800, applicationFeeCurrency: 'CNY',
    })
    expect(payload.levelStipends).toEqual([
      { level: 'MASTER', amount: 3000, currency: 'CNY', frequency: 'MONTHLY', durationMonths: 24, conditions: null },
    ])
    expect(payload.intakes).toEqual([{ term: 'AUTUMN_SEPTEMBER', applicationOpen: '2026-01-01', applicationClose: '2026-04-01' }])
  })

  describe('fields of study', () => {
    it('keeps one entry per field with its level, and trims them', () => {
      const form = blankForm()
      form.levels = ['BACHELOR', 'MASTER']
      form.fields = [
        { level: null, name: '  Engineering ' },
        { level: 'MASTER', name: 'Medicine' },
        { level: 'BACHELOR', name: '   ' },
      ]
      expect(buildPayload(form).fields).toEqual([
        { level: null, name: 'Engineering' },
        { level: 'MASTER', name: 'Medicine' },
      ])
    })

    it('does not send a field for a level that is no longer offered', () => {
      const form = blankForm()
      form.levels = ['BACHELOR']
      form.fields = [{ level: 'PHD', name: 'Physics' }, { level: null, name: 'Law' }]
      expect(buildPayload(form).fields).toEqual([{ level: null, name: 'Law' }])
    })

    it('no longer sends the old single field text', () => {
      expect(buildPayload(blankForm()).field).toBeNull()
    })

    it('loads the structured fields of a scholarship', () => {
      const loaded = formFromScholarship({
        ...scholarship,
        view: { ...scholarship.view, fields: [{ level: null, name: 'Law' }, { level: 'MASTER', name: 'Medicine' }] },
      } as Scholarship)
      expect(loaded.fields).toEqual([{ level: null, name: 'Law' }, { level: 'MASTER', name: 'Medicine' }])
    })

    it('turns an older scholarship that only has the summary text into one every-level field', () => {
      const loaded = formFromScholarship({
        ...scholarship,
        view: { ...scholarship.view, fields: [], field: 'Engineering' },
      } as Scholarship)
      expect(loaded.fields).toEqual([{ level: null, name: 'Engineering' }])
    })
  })
})
