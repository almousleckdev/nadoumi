import { describe, it, expect } from 'vitest'
import { profileCompleteness } from '~/utils/profileCompleteness'
import type { ApplicantDto } from '~/types/catalog'

const full = {
  givenName: 'Amina', familyName: 'Diallo', dob: '2000-01-01', nationality: 'SN',
  passportNo: 'A123', email: 'a@x.com', phone: '+221700000000',
} as ApplicantDto

describe('profileCompleteness', () => {
  it('is 100 when every tracked field is filled', () => {
    expect(profileCompleteness(full)).toBe(100)
  })

  it('is 0 when nothing is filled', () => {
    expect(profileCompleteness({} as ApplicantDto)).toBe(0)
  })

  it('rounds the filled share of the seven tracked fields', () => {
    expect(profileCompleteness({ ...full, passportNo: '', phone: null, email: undefined } as unknown as ApplicantDto)).toBe(57)
  })
})
