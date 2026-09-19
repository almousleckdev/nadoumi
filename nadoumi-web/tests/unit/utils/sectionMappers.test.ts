import { describe, it, expect } from 'vitest'
import {
  educationBody, emptyEducation, emptyResidence, interestsBody, residenceBody, workBody, workForm,
} from '~/utils/sectionMappers'

describe('educationBody', () => {
  it('drops the end date of a current record and turns blanks into absent fields', () => {
    const body = educationBody({
      ...emptyEducation(), institution: ' Cairo University ', country: 'EG', level: 'BACHELOR',
      startDate: '2021-09-01', endDate: '2025-06-30', current: true, gpa: '3.5', gpaScale: '4',
    })
    expect(body).toMatchObject({ institution: 'Cairo University', endDate: undefined, current: true, gpa: 3.5, gpaScale: 4 })
    expect(body.city).toBeUndefined()
  })
})

describe('workBody', () => {
  const job = { ...workForm(null), employer: 'Acme', jobTitle: 'Analyst', startDate: '2023-01-01', workVisaType: 'Z', workVisaExpiry: '2027-01-01' }

  it('keeps the work visa for a job in China', () => {
    expect(workBody({ ...job, country: 'CN' })).toMatchObject({ workVisaType: 'Z', workVisaExpiry: '2027-01-01' })
  })
  it('drops the work visa for a job anywhere else', () => {
    expect(workBody({ ...job, country: 'EG' })).toMatchObject({ workVisaType: undefined, workVisaExpiry: undefined })
  })
})

describe('residenceBody', () => {
  const china = { ...emptyResidence(), inChina: true, country: 'CN', city: 'Beijing', visaType: 'X1', visaExpiryDate: '2027-01-01', chinaEducationLevel: 'BACHELOR' }

  it('sends the China answers for a student in China', () => {
    expect(residenceBody(china)).toMatchObject({ inChina: true, visaType: 'X1', chinaEducationLevel: 'BACHELOR' })
  })
  it('leaves them out for a student elsewhere', () => {
    expect(residenceBody({ ...china, inChina: false, country: 'EG' }))
      .toMatchObject({ inChina: false, visaType: undefined, visaExpiryDate: undefined, chinaEducationLevel: undefined })
  })
})

describe('interestsBody', () => {
  it('turns the intake year into a number and blanks into absent fields', () => {
    const body = interestsBody({
      desiredLevel: 'MASTER', fields: ['LAW'], cities: ['Beijing'], scholarshipInterest: '', intakeYear: '2027',
      intakeTerm: 'FALL', teachingLanguage: '', notes: '',
    })
    expect(body).toMatchObject({ intakeYear: 2027, intakeTerm: 'FALL', scholarshipInterest: undefined })
  })
})
