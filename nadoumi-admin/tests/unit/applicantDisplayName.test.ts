import { describe, it, expect } from 'vitest'
import { applicantDisplayName } from '@/utils/applicantLabels'

describe('applicantDisplayName', () => {
  it('joins the given and family names', () => {
    expect(applicantDisplayName({ givenName: 'Amina', familyName: 'Benali', email: 'a@x.io' }, 'Not named yet')).toBe('Amina Benali')
  })

  it('falls back to the email for a sign-up that has no name yet', () => {
    expect(applicantDisplayName({ givenName: '', familyName: '', email: 'mohamed@x.io' }, 'Not named yet')).toBe('mohamed@x.io')
  })

  it('falls back to the placeholder when there is neither a name nor an email', () => {
    expect(applicantDisplayName({ givenName: null, familyName: null, email: null }, 'Not named yet')).toBe('Not named yet')
  })

  it('copes with only one of the two names', () => {
    expect(applicantDisplayName({ givenName: 'Amina', familyName: '', email: null }, 'x')).toBe('Amina')
  })
})
