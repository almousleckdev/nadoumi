import { describe, expect, it } from 'vitest'
import {
  emptyFamilyMember, familyMemberBody, personBody, personForm, personalBody, personalForm, validateFamilyMember, validatePerson,
} from '~/utils/familyForms'

const TODAY = new Date('2026-10-22T12:00:00Z')

describe('validateFamilyMember', () => {
  it('needs a relation and a name', () => {
    expect(validateFamilyMember(emptyFamilyMember(), TODAY)).toEqual({ relation: 'required', fullName: 'required' })
  })

  it('needs a note once the person has died, and not before', () => {
    const base = { ...emptyFamilyMember(), relation: 'MOTHER', fullName: 'Fatoumata' }
    expect(validateFamilyMember({ ...base, deceased: true }, TODAY)).toEqual({ note: 'required' })
    expect(validateFamilyMember({ ...base, deceased: true, note: 'Passed away in 2020' }, TODAY)).toEqual({})
    expect(validateFamilyMember(base, TODAY)).toEqual({})
  })

  it('refuses a birth date in the future and a malformed email', () => {
    const base = { ...emptyFamilyMember(), relation: 'SIBLING', fullName: 'Moussa' }
    expect(validateFamilyMember({ ...base, dateOfBirth: '2999-01-01' }, TODAY)).toEqual({ dateOfBirth: 'dobFuture' })
    expect(validateFamilyMember({ ...base, email: 'nope' }, TODAY)).toEqual({ email: 'emailInvalid' })
  })
})

describe('familyMemberBody', () => {
  it('drops blank fields and ignores a stale note for a living person', () => {
    const body = familyMemberBody({ ...emptyFamilyMember(), relation: 'FATHER', fullName: ' Ibrahima ', note: 'old', company: '  ' })
    expect(body).toEqual({
      relation: 'FATHER', fullName: 'Ibrahima', deceased: false, dateOfBirth: undefined, nationality: undefined,
      note: undefined, company: undefined, position: undefined, companyAddress: undefined, phone: undefined, email: undefined,
    })
  })
})

describe('person and personal details', () => {
  it('requires name, relationship and phone for a supporter', () => {
    expect(validatePerson(personForm(null))).toEqual({ fullName: 'required', relationship: 'required', phone: 'required' })
  })

  it('maps a person form to a trimmed body without blanks', () => {
    const body = personBody({ ...personForm(null), fullName: ' Li Wei ', relationship: 'Friend', phone: '+8613800000000' })
    expect(body.fullName).toBe('Li Wei')
    expect(body.email).toBeUndefined()
  })

  it('starts the personal form empty and sends only what was filled', () => {
    expect(personalForm(null).maritalStatus).toBe('')
    expect(personalBody({ ...personalForm(null), religion: ' Muslim ' })).toMatchObject({ religion: 'Muslim', maritalStatus: undefined })
    expect(personalForm(null).mailingChoice).toBe('CURRENT')
    const other = { ...personalForm(null), mailingChoice: 'OTHER' as const, mailingAddress: ' 5 Avenue ', mailingPostcode: '3000' }
    expect(personalBody(other)).toMatchObject({ mailingChoice: 'OTHER', mailingAddress: '5 Avenue', mailingPostcode: '3000' })
    expect(personalBody({ ...other, mailingChoice: 'PERMANENT' })).toMatchObject({ mailingAddress: undefined, mailingPostcode: undefined })
  })
})
