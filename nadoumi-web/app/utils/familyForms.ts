import type { FamilyMemberBody, PersonalDetailsBody, PersonBody } from '~/composables/useApplicantFamily'
import type { FieldErrors } from '~/constants/formErrors'
import type { FamilyMemberDto, MailingChoice, PersonalDetailsDto, PersonDto } from '~/types/family'
import { isoDate } from '~/utils/dates'

/*
 * Form state, API mapping and validation for the family sections. Pure, like `sectionRules`: error codes only,
 * mirroring the server rules in `nadoumi-applicant` (`FamilyRules`). The server stays the authority.
 */

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/
const text = (value: string | null | undefined): string => value ?? ''
const optional = (value: string): string | undefined => value.trim() || undefined
const blank = (value: string | undefined | null): boolean => !value?.trim()

export interface PersonalDetailsForm {
  maritalStatus: string; religion: string; placeOfBirth: string; officialLanguage: string
  permanentAddress: string; permanentPostcode: string
  currentPostcode: string; mailingChoice: MailingChoice; mailingAddress: string; mailingPostcode: string
}

export const personalForm = (dto: PersonalDetailsDto | null): PersonalDetailsForm => ({
  maritalStatus: text(dto?.maritalStatus), religion: text(dto?.religion), placeOfBirth: text(dto?.placeOfBirth),
  officialLanguage: text(dto?.officialLanguage), permanentAddress: text(dto?.permanentAddress),
  permanentPostcode: text(dto?.permanentPostcode), currentPostcode: text(dto?.currentPostcode),
  mailingChoice: dto?.mailingChoice ?? 'CURRENT', mailingAddress: text(dto?.mailingAddress),
  mailingPostcode: text(dto?.mailingPostcode),
})

export const personalBody = (f: PersonalDetailsForm): PersonalDetailsBody => ({
  maritalStatus: optional(f.maritalStatus), religion: optional(f.religion), placeOfBirth: optional(f.placeOfBirth),
  officialLanguage: optional(f.officialLanguage), permanentAddress: optional(f.permanentAddress),
  permanentPostcode: optional(f.permanentPostcode), currentPostcode: optional(f.currentPostcode),
  mailingChoice: f.mailingChoice,
  mailingAddress: f.mailingChoice === 'OTHER' ? optional(f.mailingAddress) : undefined,
  mailingPostcode: f.mailingChoice === 'OTHER' ? optional(f.mailingPostcode) : undefined,
})

export interface FamilyMemberForm {
  relation: string; fullName: string; dateOfBirth: string; nationality: string; deceased: boolean; note: string
  company: string; position: string; companyAddress: string; phone: string; email: string
}

export const emptyFamilyMember = (): FamilyMemberForm => ({
  relation: '', fullName: '', dateOfBirth: '', nationality: '', deceased: false, note: '', company: '',
  position: '', companyAddress: '', phone: '', email: '',
})

export const familyMemberForm = (dto: FamilyMemberDto | null): FamilyMemberForm => !dto ? emptyFamilyMember() : ({
  relation: dto.relation, fullName: dto.fullName, dateOfBirth: text(dto.dateOfBirth), nationality: text(dto.nationality),
  deceased: dto.deceased, note: text(dto.note), company: text(dto.company), position: text(dto.position),
  companyAddress: text(dto.companyAddress), phone: text(dto.phone), email: text(dto.email),
})

export const familyMemberBody = (f: FamilyMemberForm): FamilyMemberBody => ({
  relation: f.relation, fullName: f.fullName.trim(), deceased: f.deceased,
  dateOfBirth: optional(f.dateOfBirth), nationality: optional(f.nationality), note: f.deceased ? optional(f.note) : undefined,
  company: optional(f.company), position: optional(f.position), companyAddress: optional(f.companyAddress),
  phone: optional(f.phone), email: optional(f.email),
})

export function validateFamilyMember(form: FamilyMemberForm, today: Date = new Date()): FieldErrors {
  const errors: FieldErrors = {}
  if (blank(form.relation)) errors.relation = 'required'
  if (blank(form.fullName)) errors.fullName = 'required'
  if (form.dateOfBirth && form.dateOfBirth > isoDate(today)) errors.dateOfBirth = 'dobFuture'
  if (form.deceased && blank(form.note)) errors.note = 'required'
  if (!blank(form.email) && !EMAIL_PATTERN.test(form.email.trim())) errors.email = 'emailInvalid'
  return errors
}

export interface PersonForm {
  fullName: string; relationship: string; gender: string; email: string; phone: string
  position: string; company: string; companyAddress: string; homeAddress: string
}

export const personForm = (dto: PersonDto | null): PersonForm => ({
  fullName: text(dto?.fullName), relationship: text(dto?.relationship), gender: text(dto?.gender),
  email: text(dto?.email), phone: text(dto?.phone), position: text(dto?.position), company: text(dto?.company),
  companyAddress: text(dto?.companyAddress), homeAddress: text(dto?.homeAddress),
})

export const personBody = (f: PersonForm): PersonBody => ({
  fullName: f.fullName.trim(), relationship: f.relationship.trim(), phone: f.phone.trim(),
  gender: optional(f.gender), email: optional(f.email), position: optional(f.position), company: optional(f.company),
  companyAddress: optional(f.companyAddress), homeAddress: optional(f.homeAddress),
})

export function validatePerson(form: PersonForm): FieldErrors {
  const errors: FieldErrors = {}
  for (const key of ['fullName', 'relationship', 'phone'] as const) {
    if (blank(form[key])) errors[key] = 'required'
  }
  if (!blank(form.email) && !EMAIL_PATTERN.test(form.email.trim())) errors.email = 'emailInvalid'
  return errors
}
