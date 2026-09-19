import type { FieldErrors } from '~/constants/formErrors'
import { PASSPORT_MIN_VALIDITY_MONTHS, PASSPORT_NUMBER_PATTERN } from '~/constants/passport'
import { addMonths, isoDate } from '~/utils/dates'
import { isValidName } from '~/utils/profileRules'

export interface PassportForm {
  passportNo: string
  givenName: string
  familyName: string
  dob: string
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
  for (const key of ['givenName', 'familyName'] as const) {
    if (!form[key].trim()) errors[key] = 'required'
    else if (!isValidName(form[key])) errors[key] = 'nameInvalid'
  }
  if (!form.passportNo.trim()) errors.passportNo = 'required'
  else if (!PASSPORT_NUMBER_PATTERN.test(form.passportNo.trim())) errors.passportNo = 'numberInvalid'
  for (const key of ['dob', 'issueDate', 'expiryDate'] as const) {
    if (!form[key]) errors[key] = 'required'
  }
  if (!errors.issueDate && form.issueDate > isoDate(today)) errors.issueDate = 'issueFuture'
  if (!errors.expiryDate) {
    if (!errors.issueDate && form.expiryDate <= form.issueDate) errors.expiryDate = 'expiryBeforeIssue'
    else if (!isValidForAdmission(form.expiryDate, today)) errors.expiryDate = 'expiryTooSoon'
  }
  return errors
}
