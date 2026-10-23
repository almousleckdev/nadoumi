export type MailingChoice = 'CURRENT' | 'PERMANENT' | 'OTHER'

/** What the Nadoumi application form asks about a student beyond identity (`/api/student/applicants/{id}/...`). */
export interface PersonalDetailsDto {
  maritalStatus: string | null
  religion: string | null
  placeOfBirth: string | null
  officialLanguage: string | null
  permanentAddress: string | null
  permanentPostcode: string | null
  currentPostcode: string | null
  mailingChoice: MailingChoice
  mailingAddress: string | null
  mailingPostcode: string | null
}

export interface FamilyMemberDto {
  id: number
  relation: 'FATHER' | 'MOTHER' | 'SPOUSE' | 'SIBLING' | 'OTHER'
  fullName: string
  dateOfBirth: string | null
  nationality: string | null
  deceased: boolean
  note: string | null
  company: string | null
  position: string | null
  companyAddress: string | null
  phone: string | null
  email: string | null
}

/** The financial supporter or the emergency contact in China. */
export type PersonRole = 'supporter' | 'china-emergency'

export interface PersonDto {
  role: 'SUPPORTER' | 'CHINA_EMERGENCY'
  fullName: string
  relationship: string
  gender: string | null
  email: string | null
  phone: string
  position: string | null
  company: string | null
  companyAddress: string | null
  homeAddress: string | null
}

export type ReadinessCode = 'PERSONAL_DETAILS' | 'FAMILY' | 'SUPPORTER' | 'CHINA_EMERGENCY' | 'SIGNATURE'

export interface FormReadinessDto {
  missing: ReadinessCode[]
}

/** `GET /api/student/applicants/{id}/signature`: whether a signature is on file, never the image itself. */
export interface SignatureStatusDto {
  signed: boolean
  signedAt: string | null
}
