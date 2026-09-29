import type { ApplicantDto } from '~/types/catalog'

const PROFILE_FIELDS: (keyof ApplicantDto)[] = [
  'givenName', 'familyName', 'dob', 'nationality', 'passportNo', 'email', 'phone',
]

export function profileCompleteness(applicant: ApplicantDto): number {
  const filled = PROFILE_FIELDS.filter(k => Boolean(applicant[k])).length
  return Math.round((filled / PROFILE_FIELDS.length) * 100)
}
