import { describe, it, expect } from 'vitest'
import {
  blankOffering, blankProgramForm, buildProgramPayload, formFromProgram, offeringProblems, syncOfferingsToLevels,
} from '@/views/programs/programForm'
import type { Program } from '@/api/program'

const program = {
  id: 9, universityId: 3, name: 'Computer Science', nameCn: null, programType: 'DEGREE',
  levels: ['BACHELOR', 'MASTER'], field: 'Engineering', termLength: null, teachingLanguage: 'ENGLISH',
  durationMonths: 48, tuitionAmount: 30000, tuitionCurrency: 'CNY', summary: null, imageMediaId: 55,
  majors: [{ name: 'AI', nameCn: null, departmentId: 4, level: 'MASTER' }],
  intakes: [{ term: 'AUTUMN_SEPTEMBER', applicationOpen: null, applicationClose: '2026-05-01' }],
  featured: true, hot: false, status: 'ACTIVE', publishStatus: 'PUBLISHED', remark: null,
} as unknown as Program

describe('blankProgramForm', () => {
  it('starts as an active draft degree program with no levels', () => {
    expect(blankProgramForm(null)).toMatchObject({
      universityId: null, programType: 'DEGREE', levels: [], status: 'ACTIVE', publishStatus: 'DRAFT', offerings: [],
    })
  })

  it('preselects a locked university', () => {
    expect(blankProgramForm(12).universityId).toBe(12)
  })

  it('returns independent objects on every call', () => {
    const a = blankProgramForm(null)
    a.majors.push({ name: 'x', nameCn: null, departmentId: null, level: null })
    expect(blankProgramForm(null).majors).toEqual([])
  })
})

describe('buildProgramPayload', () => {
  it('trims text and turns blanks into null', () => {
    const form = blankProgramForm(3)
    Object.assign(form, { name: '  CS  ', nameCn: '  ', field: ' Eng ', summary: '' })
    expect(buildProgramPayload(form)).toMatchObject({ universityId: 3, name: 'CS', nameCn: null, field: 'Eng', summary: null })
  })

  it('keeps levels and majors for degree programs and ignores the term length', () => {
    const form = blankProgramForm(3)
    Object.assign(form, { name: 'CS', levels: ['MASTER'], termLength: 'ONE_YEAR' })
    form.majors.push({ majorId: null, name: ' AI ', nameCn: '', departmentId: 4, level: 'MASTER' })
    form.majors.push({ majorId: null, name: '  ', nameCn: null, departmentId: null, level: null })
    form.majors.push({ majorId: 12, name: 'Data Science', nameCn: '数据科学', departmentId: null, level: 'MASTER' })
    const p = buildProgramPayload(form)
    expect(p.levels).toEqual(['MASTER'])
    expect(p.termLength).toBeNull()
    expect(p.majors).toEqual([
      { majorId: null, name: 'AI', nameCn: null, departmentId: 4, level: 'MASTER' },
      // a picked catalogue major sends only its id; the catalogue supplies the title
      { majorId: 12, name: '', nameCn: null, departmentId: null, level: 'MASTER' },
    ])
  })

  it('drops levels and majors and keeps the term length for non-degree programs', () => {
    const form = blankProgramForm(3)
    Object.assign(form, { name: 'Language', programType: 'LANGUAGE', levels: ['MASTER'], termLength: 'ONE_YEAR' })
    form.majors.push({ majorId: null, name: 'AI', nameCn: null, departmentId: null, level: null })
    const p = buildProgramPayload(form)
    expect(p.levels).toEqual([])
    expect(p.majors).toEqual([])
    expect(p.termLength).toBe('ONE_YEAR')
  })

  it('drops intakes without a term and blanks empty dates', () => {
    const form = blankProgramForm(3)
    form.intakes.push({ term: '', applicationOpen: '2026-01-01', applicationClose: null })
    form.intakes.push({ term: 'SPRING_MARCH', applicationOpen: '', applicationClose: '2026-02-01' })
    expect(buildProgramPayload(form).intakes).toEqual([
      { term: 'SPRING_MARCH', applicationOpen: null, applicationClose: '2026-02-01' },
    ])
  })
})

describe('formFromProgram', () => {
  it('copies the program into the form and turns missing text into empty strings', () => {
    expect(formFromProgram(program)).toMatchObject({
      id: 9, universityId: 3, name: 'Computer Science', nameCn: '', summary: '', remark: '',
      levels: ['BACHELOR', 'MASTER'], imageMediaId: 55, publishStatus: 'PUBLISHED',
    })
  })

  it('survives a round trip through buildProgramPayload', () => {
    const p = buildProgramPayload(formFromProgram(program))
    expect(p).toMatchObject({ universityId: 3, name: 'Computer Science', levels: ['BACHELOR', 'MASTER'], imageMediaId: 55 })
    expect(p.majors).toEqual([{ majorId: null, name: 'AI', nameCn: null, departmentId: 4, level: 'MASTER' }])
    expect(p.intakes).toEqual([{ term: 'AUTUMN_SEPTEMBER', applicationOpen: null, applicationClose: '2026-05-01' }])
  })
})

describe('programme offerings', () => {
  it('keeps one offering row per selected level and drops rows of removed levels', () => {
    const form = blankProgramForm(3)
    form.levels = ['BACHELOR', 'MASTER']
    syncOfferingsToLevels(form)
    expect(form.offerings.map(o => [o.level, o.durationMonths])).toEqual([['BACHELOR', 48], ['MASTER', 24]])

    form.levels = ['MASTER']
    syncOfferingsToLevels(form)
    expect(form.offerings.map(o => o.level)).toEqual(['MASTER'])
  })

  it('does not duplicate rows or discard prices already entered', () => {
    const form = blankProgramForm(3)
    form.levels = ['MASTER']
    syncOfferingsToLevels(form)
    form.offerings[0]!.tuitionAmount = 26000
    syncOfferingsToLevels(form)
    expect(form.offerings).toHaveLength(1)
    expect(form.offerings[0]!.tuitionAmount).toBe(26000)
  })

  it('gives non-degree programmes level-less rows', () => {
    expect(blankOffering(null)).toMatchObject({ level: null, durationMonths: 12, tuitionPeriod: 'PER_YEAR', accommodationPeriod: 'PER_MONTH' })
    const form = blankProgramForm(3)
    form.programType = 'LANGUAGE'
    form.levels = ['MASTER']
    syncOfferingsToLevels(form)
    expect(form.offerings).toEqual([])
  })

  it('reports durations below the level minimum and duplicate rows', () => {
    const form = blankProgramForm(3)
    form.levels = ['BACHELOR', 'PHD']
    form.offerings = [
      { ...blankOffering('BACHELOR'), durationMonths: 36 },
      { ...blankOffering('PHD'), durationMonths: 36 },
      { ...blankOffering('PHD'), durationMonths: 36 },
    ]
    expect(offeringProblems(form)).toEqual([
      { kind: 'TOO_SHORT', level: 'BACHELOR', min: 48 },
      { kind: 'DUPLICATE', level: 'PHD', min: 0 },
    ])
  })

  it('sends offerings, derives nothing locally, and trims notes', () => {
    const form = blankProgramForm(3)
    Object.assign(form, { name: 'CS', levels: ['MASTER'] })
    form.offerings = [{ ...blankOffering('MASTER'), tuitionAmount: 26000, accommodationAmount: 1200, note: '  shared  ' }]
    const p = buildProgramPayload(form)
    expect(p.offerings).toEqual([{
      level: 'MASTER', durationMonths: 24, tuitionAmount: 26000, tuitionCurrency: 'CNY', tuitionPeriod: 'PER_YEAR',
      accommodationAmount: 1200, accommodationCurrency: 'CNY', accommodationPeriod: 'PER_MONTH', note: 'shared',
    }])
  })

  it('round-trips offerings and admission fields from a loaded programme', () => {
    const loaded = {
      ...program,
      admissionRequirements: 'Bachelor degree', applicationDocuments: 'Passport', minHsk: 4, minIelts: 6, minToefl: 80,
      applicationFeeAmount: 600, careerProspects: 'x', scholarshipInfo: 'y',
      offerings: [{ level: 'MASTER', durationMonths: 24, tuitionAmount: 26000, tuitionCurrency: 'CNY', tuitionPeriod: 'PER_YEAR',
        accommodationAmount: null, accommodationCurrency: null, accommodationPeriod: 'PER_MONTH', note: null }],
    } as unknown as Program
    const form = formFromProgram(loaded)
    expect(form.offerings[0]).toMatchObject({ level: 'MASTER', durationMonths: 24, tuitionAmount: 26000, note: '' })
    expect(form).toMatchObject({ minHsk: 4, minIelts: 6, minToefl: 80, applicationFeeAmount: 600, admissionRequirements: 'Bachelor degree' })
    expect(buildProgramPayload(form)).toMatchObject({ minHsk: 4, applicationFeeAmount: 600, applicationFeeCurrency: 'CNY' })
  })
})

