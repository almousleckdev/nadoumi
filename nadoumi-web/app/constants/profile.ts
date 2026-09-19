import type { ApplicantDto } from '~/types/catalog'

export const GENDERS = ['FEMALE', 'MALE', 'UNSPECIFIED'] as const
export type Gender = typeof GENDERS[number]

/** Profile text fields that must be non-empty before onboarding can finish. */
export const REQUIRED_PROFILE_FIELDS = [
  'gender', 'nationality', 'countryOfOrigin', 'countryOfResidence', 'nativeLanguage', 'phone',
] as const satisfies readonly (keyof ApplicantDto)[]

export type ProfileErrorCode =
  | 'required' | 'nameInvalid' | 'dobFuture' | 'dobUnderage' | 'contactHandleRequired' | 'verifyBeforeSave'

/** i18n key for each validation code, so copy lives in one place. */
export const PROFILE_ERROR_KEYS: Record<ProfileErrorCode, string> = {
  required: 'validation.required',
  nameInvalid: 'profileForm.nameInvalid',
  dobFuture: 'profileForm.dobFuture',
  dobUnderage: 'profileForm.dobUnderage',
  contactHandleRequired: 'profileForm.contactHandleRequired',
  verifyBeforeSave: 'profileForm.verifyBeforeSave',
}
