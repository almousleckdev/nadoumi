import { describe, it, expect } from 'vitest'
import {
  validateContact, validateEducation, validateInterests, validateResidence, validateWork,
  type ContactForm, type EducationForm, type InterestsForm, type ResidenceForm, type WorkForm,
} from '~/utils/sectionRules'

const TODAY = new Date(2026, 8, 19)

const education = (over: Partial<EducationForm> = {}): EducationForm => ({
  institution: 'Cairo University', country: 'EG', city: '', level: 'BACHELOR', qualification: '', field: '',
  gpa: '', gpaScale: '', startDate: '2021-09-01', endDate: '2025-06-30', current: false, ...over,
})
const work = (over: Partial<WorkForm> = {}): WorkForm => ({
  employer: 'Acme', jobTitle: 'Analyst', employmentType: '', country: 'EG', city: '', startDate: '2023-01-01',
  endDate: '2024-01-01', current: false, description: '', workVisaType: '', workVisaExpiry: '', ...over,
})
const residence = (over: Partial<ResidenceForm> = {}): ResidenceForm => ({
  inChina: false, country: 'EG', city: 'Cairo', address: '', chinaEducationLevel: '', chinaSchool: '',
  visaType: '', visaExpiryDate: '', ...over,
})
const interests = (over: Partial<InterestsForm> = {}): InterestsForm => ({
  desiredLevel: 'MASTER', fields: ['LAW'], cities: ['Beijing'], scholarshipInterest: '', intakeYear: '',
  intakeTerm: '', teachingLanguage: '', notes: '', ...over,
})
const contact = (over: Partial<ContactForm> = {}): ContactForm => ({
  relation: 'GUARDIAN', name: 'Sam', email: '', phone: '+201000000000', ...over,
})

describe('validateEducation', () => {
  it('accepts a finished record', () => {
    expect(validateEducation(education(), TODAY)).toEqual({})
  })
  it('requires institution, country, level and start date', () => {
    const errors = validateEducation(education({ institution: ' ', country: '', level: '', startDate: '' }), TODAY)
    expect(Object.keys(errors).sort()).toEqual(['country', 'institution', 'level', 'startDate'])
  })
  it('rejects a start in the future and an end before the start', () => {
    expect(validateEducation(education({ startDate: '2027-01-01' }), TODAY).startDate).toBe('startFuture')
    expect(validateEducation(education({ endDate: '2021-01-01' }), TODAY).endDate).toBe('endBeforeStart')
  })
  it('lets a current record stay open but not a finished one end in the future', () => {
    expect(validateEducation(education({ current: true, endDate: '' }), TODAY)).toEqual({})
    expect(validateEducation(education({ endDate: '2027-06-30' }), TODAY).endDate).toBe('endFuture')
  })
  it('asks for an end date when the record is finished', () => {
    expect(validateEducation(education({ endDate: '' }), TODAY).endDate).toBe('endRequired')
  })
})

describe('validateWork', () => {
  it('accepts a finished job outside China', () => {
    expect(validateWork(work(), TODAY)).toEqual({})
  })
  it('needs the work visa type for a job in China', () => {
    expect(validateWork(work({ country: 'CN' }), TODAY).workVisaType).toBe('required')
    expect(validateWork(work({ country: 'CN', workVisaType: 'Z' }), TODAY)).toEqual({})
  })
  it('needs employer, title, country and start', () => {
    const errors = validateWork(work({ employer: '', jobTitle: '', country: '', startDate: '' }), TODAY)
    expect(Object.keys(errors).sort()).toEqual(['country', 'employer', 'jobTitle', 'startDate'])
  })
})

describe('validateResidence', () => {
  it('asks the China question first', () => {
    expect(validateResidence(residence({ inChina: null }), TODAY)).toEqual({ inChina: 'required' })
  })
  it('accepts a student abroad with only country and city', () => {
    expect(validateResidence(residence(), TODAY)).toEqual({})
  })
  it('requires level, visa type and expiry in China, and an unexpired visa', () => {
    const base = residence({ inChina: true, country: 'CN', city: 'Beijing' })
    expect(Object.keys(validateResidence(base, TODAY)).sort())
      .toEqual(['chinaEducationLevel', 'visaExpiryDate', 'visaType'])
    const lapsed = { ...base, chinaEducationLevel: 'BACHELOR', visaType: 'X1', visaExpiryDate: '2026-09-19' }
    expect(validateResidence(lapsed, TODAY).visaExpiryDate).toBe('visaExpired')
    expect(validateResidence({ ...lapsed, visaExpiryDate: '2027-01-01' }, TODAY)).toEqual({})
  })
})

describe('validateInterests', () => {
  it('accepts a level with a field and a city', () => {
    expect(validateInterests(interests())).toEqual({})
  })
  it('needs a level, at least one field and one city', () => {
    expect(validateInterests(interests({ desiredLevel: '', fields: [], cities: [] })))
      .toEqual({ desiredLevel: 'required', fields: 'needOne', cities: 'needOne' })
  })
})

describe('validateContact', () => {
  it('accepts a guardian with a phone', () => {
    expect(validateContact(contact())).toEqual({})
  })
  it('requires a phone for guardians and emergency contacts only', () => {
    expect(validateContact(contact({ phone: '' })).phone).toBe('required')
    expect(validateContact(contact({ relation: 'OTHER', phone: '' }))).toEqual({})
  })
  it('rejects a malformed email but allows none', () => {
    expect(validateContact(contact({ email: 'nope' })).email).toBe('emailInvalid')
    expect(validateContact(contact({ email: 'a@b.co' }))).toEqual({})
  })
})
