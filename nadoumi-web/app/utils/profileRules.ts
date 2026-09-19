import type { SelfApplicantBody } from '~/composables/useApplicant'
import type { FieldErrors } from '~/constants/formErrors'
import { REQUIRED_PROFILE_FIELDS } from '~/constants/profile'
import { isoDate } from '~/utils/dates'

/** Applicants must be at least this old (kept in step with the backend `AgeRules`). */
export const MINIMUM_AGE_YEARS = 17

/** The latest date of birth that is still old enough, as an ISO date (the input's `max`). */
export function latestEligibleDob(today: Date = new Date()): string {
  return isoDate(new Date(today.getFullYear() - MINIMUM_AGE_YEARS, today.getMonth(), today.getDate()))
}

export type DobProblem = 'required' | 'future' | 'underage' | null

export function dobProblem(dob: string, today: Date = new Date()): DobProblem {
  if (!dob) return 'required'
  if (dob > isoDate(today)) return 'future'
  if (dob > latestEligibleDob(today)) return 'underage'
  return null
}

const NAME_PATTERN = /^\p{L}[\p{L} '’-]*$/u

/** Passport-style names: letters (any script), spaces, hyphens and apostrophes. */
export const isValidName = (name: string): boolean => NAME_PATTERN.test(name.trim())

/** Mirrors the server's normalisation so the field shows what will be stored. */
export const normaliseName = (name: string): string =>
  name.trim().replace(/\s+/g, ' ').replace(/’/g, '\'').toUpperCase()

/**
 * Field-level validation for the profile form. Pure: returns codes, not copy, so the
 * component translates them in one place and this stays unit-testable.
 */
export function validateProfile(
  form: SelfApplicantBody,
  options: { emailNeedsVerification: boolean, today?: Date },
): FieldErrors {
  const errors: FieldErrors = {}
  for (const key of ['givenName', 'familyName'] as const) {
    if (!form[key]?.trim()) errors[key] = 'required'
    else if (!isValidName(form[key])) errors[key] = 'nameInvalid'
  }
  const dob = dobProblem(form.dob ?? '', options.today)
  if (dob === 'required') errors.dob = 'required'
  else if (dob === 'future') errors.dob = 'dobFuture'
  else if (dob === 'underage') errors.dob = 'dobUnderage'
  for (const key of REQUIRED_PROFILE_FIELDS) {
    if (!form[key]?.trim()) errors[key] = 'required'
  }
  if (!form.wechatId?.trim() && !form.whatsapp?.trim()) errors.contactHandle = 'contactHandleRequired'
  if (!form.email?.trim()) errors.email = 'required'
  else if (options.emailNeedsVerification) errors.email = 'verifyBeforeSave'
  return errors
}
