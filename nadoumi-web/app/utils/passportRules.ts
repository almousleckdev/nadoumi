import {
  PASSPORT_MIN_VALIDITY_MONTHS, PASSPORT_NUMBER_PATTERN, type PassportErrorCode,
} from '~/constants/passport'
import { isValidName } from '~/utils/profileRules'

export interface PassportForm {
  passportNo: string
  givenName: string
  familyName: string
  dob: string
  issueDate: string
  expiryDate: string
}

const pad = (n: number) => String(n).padStart(2, '0')
const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

/** `today` plus whole months, clamped to the end of the month like the server's `LocalDate.plusMonths`. */
export function addMonths(today: Date, months: number): string {
  const first = new Date(today.getFullYear(), today.getMonth() + months, 1)
  const lastDay = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate()
  return iso(new Date(first.getFullYear(), first.getMonth(), Math.min(today.getDate(), lastDay)))
}

/** True when the passport stays valid for MORE than six months from `today`. */
export function isValidForAdmission(expiryDate: string, today: Date = new Date()): boolean {
  return Boolean(expiryDate) && expiryDate > addMonths(today, PASSPORT_MIN_VALIDITY_MONTHS)
}

export type PassportErrors = Partial<Record<keyof PassportForm, PassportErrorCode>>

/** Field validation for the passport details; pure, mirrors the server rules. */
export function validatePassport(form: PassportForm, today: Date = new Date()): PassportErrors {
  const errors: PassportErrors = {}
  for (const key of ['givenName', 'familyName'] as const) {
    if (!form[key].trim()) errors[key] = 'required'
    else if (!isValidName(form[key])) errors[key] = 'nameInvalid'
  }
  if (!form.passportNo.trim()) errors.passportNo = 'required'
  else if (!PASSPORT_NUMBER_PATTERN.test(form.passportNo.trim())) errors.passportNo = 'numberInvalid'
  for (const key of ['dob', 'issueDate', 'expiryDate'] as const) {
    if (!form[key]) errors[key] = 'required'
  }
  if (!errors.issueDate && form.issueDate > iso(today)) errors.issueDate = 'issueFuture'
  if (!errors.expiryDate) {
    if (!errors.issueDate && form.expiryDate <= form.issueDate) errors.expiryDate = 'expiryBeforeIssue'
    else if (!isValidForAdmission(form.expiryDate, today)) errors.expiryDate = 'expiryTooSoon'
  }
  return errors
}
