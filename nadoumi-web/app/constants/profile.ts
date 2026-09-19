import type { ApplicantDto } from '~/types/catalog'

export const GENDERS = ['FEMALE', 'MALE', 'UNSPECIFIED'] as const
export type Gender = typeof GENDERS[number]

/** Profile text fields that must be non-empty before onboarding can finish. */
export const REQUIRED_PROFILE_FIELDS = [
  'gender', 'nationality', 'countryOfOrigin', 'countryOfResidence', 'nativeLanguage', 'phone',
] as const satisfies readonly (keyof ApplicantDto)[]
