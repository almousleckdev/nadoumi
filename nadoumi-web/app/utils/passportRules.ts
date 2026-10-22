import type { FieldErrors } from '~/constants/formErrors'
import { PASSPORT_MIN_VALIDITY_MONTHS, PASSPORT_NUMBER_PATTERN } from '~/constants/passport'
import { addMonths, isoDate } from '~/utils/dates'

/** What the student types off the passport; the holder's name and birth date come from the profile. */
export interface PassportForm {
  passportNo: string
  issueDate: string
  expiryDate: string
}

/** True when the passport stays valid for MORE than six months from `today`. */
export function isValidForAdmission(expiryDate: string, today: Date = new Date()): boolean {
  return Boolean(expiryDate) && expiryDate > addMonths(today, PASSPORT_MIN_VALIDITY_MONTHS)
}

/** Field validation for the passport details; pure, mirrors the server rules. */
export function validatePassport(form: PassportForm, today: Date = new Date()): FieldErrors {
  const errors: FieldErrors = {}
  if (!form.passportNo.trim()) errors.passportNo = 'required'
  else if (!PASSPORT_NUMBER_PATTERN.test(form.passportNo.trim())) errors.passportNo = 'numberInvalid'
  for (const key of ['issueDate', 'expiryDate'] as const) {
    if (!form[key]) errors[key] = 'required'
  }
  if (!errors.issueDate && form.issueDate > isoDate(today)) errors.issueDate = 'issueFuture'
  if (!errors.expiryDate) {
    if (!errors.issueDate && form.expiryDate <= form.issueDate) errors.expiryDate = 'expiryBeforeIssue'
    else if (!isValidForAdmission(form.expiryDate, today)) errors.expiryDate = 'expiryTooSoon'
  }
  return errors
}
