import { describe, it, expect } from 'vitest'
import { isProfileComplete, ONBOARDING_REQUIRED } from '~/composables/useOnboarding'
import type { ApplicantDto } from '~/types/catalog'

function applicant(over: Partial<ApplicantDto> = {}): ApplicantDto {
  return {
    id: 1, givenName: 'Ada', familyName: 'Lovelace', dob: '1990-01-01',
    nationality: 'GB', passportNo: 'P1', email: 'a@b.c', phone: '+100',
    status: 'ACTIVE', ...over,
  } as ApplicantDto
}

describe('isProfileComplete', () => {
  it('is true only when every core identity field is present', () => {
    expect(isProfileComplete(applicant())).toBe(true)
  })

  it('is false when a required field is missing or blank', () => {
    for (const k of ONBOARDING_REQUIRED) {
      expect(isProfileComplete(applicant({ [k]: null } as Partial<ApplicantDto>))).toBe(false)
      expect(isProfileComplete(applicant({ [k]: '' } as Partial<ApplicantDto>))).toBe(false)
    }
  })

  it('is false for a null/absent applicant', () => {
    expect(isProfileComplete(null)).toBe(false)
    expect(isProfileComplete(undefined)).toBe(false)
  })

  it('does not require email (it is verified at registration, not part of the gate)', () => {
    expect(isProfileComplete(applicant({ email: null }))).toBe(true)
  })
})
