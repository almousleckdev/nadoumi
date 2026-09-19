import type { ContactBody, EducationBody, InterestBody, ResidenceBody, WorkBody } from '~/composables/useApplicant'
import { CHINA_COUNTRY } from '~/constants/applicantOptions'
import type { ContactDto, EducationDto, InterestDto, ResidenceDto, WorkDto } from '~/types/catalog'
import type {
  ContactForm, EducationForm, InterestsForm, ResidenceForm, WorkForm,
} from '~/utils/sectionRules'

/** Form state uses strings everywhere (inputs are strings); these map to and from the API shapes. */

const text = (value: string | null | undefined): string => value ?? ''
const optional = (value: string): string | undefined => value.trim() || undefined
const optionalNumber = (value: string): number | undefined => (value.trim() ? Number(value) : undefined)
const numberText = (value: number | null | undefined): string => (value == null ? '' : String(value))

export const emptyEducation = (): EducationForm => ({
  institution: '', country: '', city: '', level: '', qualification: '', field: '',
  gpa: '', gpaScale: '', startDate: '', endDate: '', current: false,
})

export const educationForm = (dto: EducationDto | null): EducationForm => !dto ? emptyEducation() : ({
  institution: dto.institution, country: dto.country, city: text(dto.city), level: dto.level,
  qualification: text(dto.qualification), field: text(dto.field), gpa: numberText(dto.gpa),
  gpaScale: numberText(dto.gpaScale), startDate: text(dto.startDate), endDate: text(dto.endDate), current: dto.current,
})

export const educationBody = (form: EducationForm): EducationBody => ({
  institution: form.institution.trim(), country: form.country, level: form.level,
  city: optional(form.city), qualification: optional(form.qualification), field: optional(form.field),
  gpa: optionalNumber(form.gpa), gpaScale: optionalNumber(form.gpaScale),
  startDate: optional(form.startDate), endDate: form.current ? undefined : optional(form.endDate),
  current: form.current,
})

export const emptyWork = (): WorkForm => ({
  employer: '', jobTitle: '', employmentType: '', country: '', city: '', startDate: '', endDate: '',
  current: false, description: '', workVisaType: '', workVisaExpiry: '',
})

export const workForm = (dto: WorkDto | null): WorkForm => !dto ? emptyWork() : ({
  employer: dto.employer, jobTitle: dto.jobTitle, employmentType: text(dto.employmentType), country: dto.country,
  city: text(dto.city), startDate: dto.startDate, endDate: text(dto.endDate), current: dto.current,
  description: text(dto.description), workVisaType: text(dto.workVisaType), workVisaExpiry: text(dto.workVisaExpiry),
})

/** The work visa only belongs to a job in China; it is dropped for any other country. */
export const workBody = (form: WorkForm): WorkBody => {
  const inChina = form.country === CHINA_COUNTRY
  return {
    employer: form.employer.trim(), jobTitle: form.jobTitle.trim(), country: form.country,
    startDate: form.startDate, current: form.current, employmentType: optional(form.employmentType),
    city: optional(form.city), endDate: form.current ? undefined : optional(form.endDate),
    description: optional(form.description),
    workVisaType: inChina ? optional(form.workVisaType) : undefined,
    workVisaExpiry: inChina ? optional(form.workVisaExpiry) : undefined,
  }
}

export const emptyContact = (): ContactForm => ({ relation: '', name: '', email: '', phone: '' })

export const contactForm = (dto: ContactDto | null): ContactForm => !dto ? emptyContact() : ({
  relation: dto.relation, name: dto.name, email: text(dto.email), phone: text(dto.phone),
})

export const contactBody = (form: ContactForm): ContactBody => ({
  relation: form.relation, name: form.name.trim(), email: optional(form.email), phone: optional(form.phone),
})

export const emptyResidence = (): ResidenceForm => ({
  inChina: null, country: '', city: '', address: '', chinaEducationLevel: '', chinaSchool: '',
  visaType: '', visaExpiryDate: '',
})

export const residenceForm = (dto: ResidenceDto | null): ResidenceForm => !dto ? emptyResidence() : ({
  inChina: dto.inChina, country: dto.country, city: dto.city, address: text(dto.address),
  chinaEducationLevel: text(dto.chinaEducationLevel), chinaSchool: text(dto.chinaSchool),
  visaType: text(dto.visaType), visaExpiryDate: text(dto.visaExpiryDate),
})

/** The China-only answers are not sent for a student living elsewhere. */
export const residenceBody = (form: ResidenceForm): ResidenceBody => {
  const inChina = form.inChina === true
  return {
    inChina, country: form.country, city: form.city.trim(), address: optional(form.address),
    chinaEducationLevel: inChina ? optional(form.chinaEducationLevel) : undefined,
    chinaSchool: inChina ? optional(form.chinaSchool) : undefined,
    visaType: inChina ? optional(form.visaType) : undefined,
    visaExpiryDate: inChina ? optional(form.visaExpiryDate) : undefined,
  }
}

export const emptyInterests = (): InterestsForm => ({
  desiredLevel: '', fields: [], cities: [], scholarshipInterest: '', intakeYear: '', intakeTerm: '',
  teachingLanguage: '', notes: '',
})

export const interestsForm = (dto: InterestDto | null): InterestsForm => !dto ? emptyInterests() : ({
  desiredLevel: dto.desiredLevel, fields: [...dto.fields], cities: [...dto.cities],
  scholarshipInterest: text(dto.scholarshipInterest), intakeYear: numberText(dto.intakeYear),
  intakeTerm: text(dto.intakeTerm), teachingLanguage: text(dto.teachingLanguage), notes: text(dto.notes),
})

export const interestsBody = (form: InterestsForm): InterestBody => ({
  desiredLevel: form.desiredLevel, fields: form.fields, cities: form.cities,
  scholarshipInterest: optional(form.scholarshipInterest), intakeYear: optionalNumber(form.intakeYear),
  intakeTerm: optional(form.intakeTerm), teachingLanguage: optional(form.teachingLanguage), notes: optional(form.notes),
})
