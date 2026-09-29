import { describe, it, expect } from 'vitest'
import { blankProgramForm, buildProgramPayload, formFromProgram } from '@/views/programs/programForm'
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
      universityId: null, programType: 'DEGREE', levels: [], status: 'ACTIVE', publishStatus: 'DRAFT', tuitionCurrency: 'CNY',
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
    form.majors.push({ name: ' AI ', nameCn: '', departmentId: 4, level: 'MASTER' })
    form.majors.push({ name: '  ', nameCn: null, departmentId: null, level: null })
    const p = buildProgramPayload(form)
    expect(p.levels).toEqual(['MASTER'])
    expect(p.termLength).toBeNull()
    expect(p.majors).toEqual([{ name: 'AI', nameCn: null, departmentId: 4, level: 'MASTER' }])
  })

  it('drops levels and majors and keeps the term length for non-degree programs', () => {
    const form = blankProgramForm(3)
    Object.assign(form, { name: 'Language', programType: 'LANGUAGE', levels: ['MASTER'], termLength: 'ONE_YEAR' })
    form.majors.push({ name: 'AI', nameCn: null, departmentId: null, level: null })
    const p = buildProgramPayload(form)
    expect(p.levels).toEqual([])
    expect(p.majors).toEqual([])
    expect(p.termLength).toBe('ONE_YEAR')
  })

  it('sets the tuition currency only when a tuition amount is present', () => {
    const form = blankProgramForm(3)
    expect(buildProgramPayload(form)).toMatchObject({ tuitionAmount: null, tuitionCurrency: null })
    form.tuitionAmount = 30000
    expect(buildProgramPayload(form)).toMatchObject({ tuitionAmount: 30000, tuitionCurrency: 'CNY' })
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
      levels: ['BACHELOR', 'MASTER'], imageMediaId: 55, tuitionAmount: 30000, publishStatus: 'PUBLISHED',
    })
  })

  it('survives a round trip through buildProgramPayload', () => {
    const p = buildProgramPayload(formFromProgram(program))
    expect(p).toMatchObject({ universityId: 3, name: 'Computer Science', levels: ['BACHELOR', 'MASTER'], imageMediaId: 55 })
    expect(p.majors).toEqual([{ name: 'AI', nameCn: null, departmentId: 4, level: 'MASTER' }])
    expect(p.intakes).toEqual([{ term: 'AUTUMN_SEPTEMBER', applicationOpen: null, applicationClose: '2026-05-01' }])
  })
})
