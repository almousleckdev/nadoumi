import { CHINA_COUNTRY, CONTACT_RELATIONS_REACHABLE } from '~/constants/applicantOptions'
import type { FieldErrors } from '~/constants/formErrors'
import { isoDate } from '~/utils/dates'

/*
 * Field validation for the onboarding sections. Pure: each returns error codes (never copy) and
 * mirrors the server rules in `nadoumi-applicant` (`EducationRules`, `WorkRules`, `ResidenceRules`).
 * The server stays the authority; these only spare the student a round trip.
 */

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/
const blank = (value: string | undefined | null): boolean => !value?.trim()

function requireFields<T extends object>(form: T, keys: readonly (keyof T)[], errors: FieldErrors) {
  for (const key of keys) {
    if (blank(form[key] as string)) errors[key as string] = 'required'
  }
}

/** Start and end dates of a record that may still be running (education and work). */
function checkPeriod(
  form: { startDate: string, endDate: string, current: boolean }, today: string, errors: FieldErrors,
) {
  if (form.startDate > today) errors.startDate = 'startFuture'
  if (form.current) return
  if (!form.endDate) errors.endDate = 'endRequired'
  else if (form.startDate && form.endDate < form.startDate) errors.endDate = 'endBeforeStart'
  else if (form.endDate > today) errors.endDate = 'endFuture'
}

export interface EducationForm {
  institution: string
  country: string
  city: string
  level: string
  qualification: string
  field: string
  gpa: string
  gpaScale: string
  startDate: string
  endDate: string
  current: boolean
}

export function validateEducation(form: EducationForm, today: Date = new Date()): FieldErrors {
  const errors: FieldErrors = {}
  requireFields(form, ['institution', 'country', 'level', 'startDate'], errors)
  checkPeriod(form, isoDate(today), errors)
  return errors
}

export interface WorkForm {
  employer: string
  jobTitle: string
  employmentType: string
  country: string
  city: string
  startDate: string
  endDate: string
  current: boolean
  description: string
  workVisaType: string
  workVisaExpiry: string
}

export function validateWork(form: WorkForm, today: Date = new Date()): FieldErrors {
  const errors: FieldErrors = {}
  requireFields(form, ['employer', 'jobTitle', 'country', 'startDate'], errors)
  checkPeriod(form, isoDate(today), errors)
  if (form.country === CHINA_COUNTRY && blank(form.workVisaType)) errors.workVisaType = 'required'
  return errors
}

export interface ResidenceForm {
  /** `null` until the student answers the question. */
  inChina: boolean | null
  country: string
  city: string
  address: string
  chinaEducationLevel: string
  chinaSchool: string
  visaType: string
  visaExpiryDate: string
}

export function validateResidence(form: ResidenceForm, today: Date = new Date()): FieldErrors {
  const errors: FieldErrors = {}
  if (form.inChina === null) {
    errors.inChina = 'required'
    return errors
  }
  requireFields(form, ['country', 'city'], errors)
  if (!form.inChina) return errors
  requireFields(form, ['chinaEducationLevel', 'visaType', 'visaExpiryDate'], errors)
  if (form.visaExpiryDate && form.visaExpiryDate <= isoDate(today)) errors.visaExpiryDate = 'visaExpired'
  return errors
}

export interface InterestsForm {
  desiredLevel: string
  fields: string[]
  cities: string[]
  scholarshipInterest: string
  intakeYear: string
  intakeTerm: string
  teachingLanguage: string
  notes: string
}

export function validateInterests(form: InterestsForm): FieldErrors {
  const errors: FieldErrors = {}
  if (blank(form.desiredLevel)) errors.desiredLevel = 'required'
  if (form.fields.length === 0) errors.fields = 'needOne'
  if (form.cities.length === 0) errors.cities = 'needOne'
  return errors
}

export interface ContactForm {
  relation: string
  name: string
  email: string
  phone: string
}

/** A guardian or emergency contact must be reachable by phone, or the section cannot complete. */
export function validateContact(form: ContactForm): FieldErrors {
  const errors: FieldErrors = {}
  requireFields(form, ['relation', 'name'], errors)
  const needsPhone = (CONTACT_RELATIONS_REACHABLE as readonly string[]).includes(form.relation)
  if (needsPhone && blank(form.phone)) errors.phone = 'required'
  if (!blank(form.email) && !EMAIL_PATTERN.test(form.email.trim())) errors.email = 'emailInvalid'
  return errors
}
