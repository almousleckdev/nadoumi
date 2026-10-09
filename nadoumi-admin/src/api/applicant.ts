import request from '@/utils/request'
import { uploadMedia } from './media'

export type ApplicantStatus = 'DRAFT' | 'ACTIVE' | 'UNLINKED' | 'ARCHIVED'

export interface Applicant {
  id: number
  givenName: string
  familyName: string
  /** masked ("••••") unless the caller holds nad:applicant:pii:view */
  dob: string | null
  nationality: string | null
  /** masked unless the caller holds nad:applicant:pii:view */
  passportNo: string | null
  email: string | null
  phone: string | null
  status: ApplicantStatus
  createdAt: string | null
  gender: string | null
  countryOfOrigin: string | null
  countryOfResidence: string | null
  nativeLanguage: string | null
  wechatId: string | null
  whatsapp: string | null
  emailVerified: boolean
  onboardingComplete: boolean
  /** signed avatar link; present only when the applicant has a profile photo */
  photoUrl?: string | null
}

/** Back-compat alias used by list views. */
export type ApplicantRow = Applicant

export interface Education {
  id: number
  institution: string
  country: string | null
  city: string | null
  level: string | null
  qualification: string | null
  field: string | null
  gpa: number | null
  gpaScale: number | null
  startDate: string | null
  endDate: string | null
  current: boolean
}

export interface Contact {
  id: number
  relation: string
  name: string
  email: string | null
  phone: string | null
}

export interface AccessGrant {
  id: number
  userId: number | null
  applicantId: number
  applicationId: number | null
  accessRole: string
  status: string
  invitedEmail: string | null
  interim: boolean
  grantedAt: string | null
  expiresAt: string | null
  revokedAt: string | null
  revokeReason: string | null
  effectiveCapabilities: string[]
}

export interface ApplicantProfileInput {
  givenName: string
  familyName: string
  dob?: string | null
  nationality?: string | null
  passportNo?: string | null
  email?: string | null
  phone?: string | null
}

export interface EducationInput {
  institution: string
  level?: string | null
  field?: string | null
  gpa?: number | null
  gpaScale?: number | null
  startDate?: string | null
  endDate?: string | null
}

export interface ContactInput {
  relation: string
  name: string
  email?: string | null
  phone?: string | null
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

const BASE = '/api/staff/applicants'

export const listApplicants = (params: {
  name?: string
  status?: string
  nationality?: string
  createdAfter?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<Applicant>>(BASE, { params })

export const getApplicant = (id: number | string) =>
  request.get<unknown, Applicant>(`${BASE}/${id}`)

export const createApplicant = (body: {
  givenName: string
  familyName: string
  invitedEmail: string
  dob?: string
  nationality?: string
  email?: string
  phone?: string
}) => request.post<unknown, Applicant>(BASE, body)

export const updateApplicant = (id: number | string, body: ApplicantProfileInput) =>
  request.put<unknown, Applicant>(`${BASE}/${id}`, body)

/** Permanent delete: the applicant, its applications, documents, files and a student login left without one. */
export const deleteApplicant = (id: number | string) =>
  request.delete(`${BASE}/${id}`)

// ---- education ----
export const listEducation = (id: number | string) =>
  request.get<unknown, Education[]>(`${BASE}/${id}/education`)
export const addEducation = (id: number | string, body: EducationInput) =>
  request.post<unknown, Education>(`${BASE}/${id}/education`, body)
export const updateEducation = (id: number | string, eduId: number, body: EducationInput) =>
  request.put<unknown, Education>(`${BASE}/${id}/education/${eduId}`, body)
export const deleteEducation = (id: number | string, eduId: number) =>
  request.delete(`${BASE}/${id}/education/${eduId}`)


// ---- contacts ----
export const listContacts = (id: number | string) =>
  request.get<unknown, Contact[]>(`${BASE}/${id}/contacts`)
export const addContact = (id: number | string, body: ContactInput) =>
  request.post<unknown, Contact>(`${BASE}/${id}/contacts`, body)
export const updateContact = (id: number | string, contactId: number, body: ContactInput) =>
  request.put<unknown, Contact>(`${BASE}/${id}/contacts/${contactId}`, body)
export const deleteContact = (id: number | string, contactId: number) =>
  request.delete(`${BASE}/${id}/contacts/${contactId}`)

// ---- access / delegation ----
export const listAccess = (id: number | string) =>
  request.get<unknown, AccessGrant[]>(`${BASE}/${id}/access`)

// ---- photo (protected asset) ----
/** Short-lived signed URL for displaying the applicant photo. Re-fetch on reload. */
export interface ApplicantPhotoUrl {
  url: string
  expiresAt: string
}

/** Upload the applicant photo. Protected — the response carries `mediaId` but no URL. */
export const uploadApplicantPhoto = (id: number | string, file: File): Promise<{ mediaId: number }> =>
  uploadMedia(`${BASE}/${id}/photo`, file).then(r => ({ mediaId: r.mediaId }))

/** Resolve a short-lived signed URL to display the applicant photo. */
export const getApplicantPhotoUrl = (id: number | string) =>
  request.get<unknown, ApplicantPhotoUrl>(`${BASE}/${id}/photo`, { params: { json: 1 } })

// ---- interests, location and work: what the student entered during onboarding (read-only for staff) ----
export interface ApplicantInterest {
  desiredLevel: string
  /** Free text typed by the student. */
  fields: string[]
  /** Provinces or cities typed by the student (at least three). */
  cities: string[]
  scholarshipInterest: string | null
  intakeYear: number | null
  intakeTerm: string | null
  /** EN, ZH or EN_ZH. */
  teachingLanguage: string | null
  notes: string | null
}
/** 204 (nothing entered yet) arrives as an empty body. */
export const getInterests = (id: number | string) =>
  request.get<unknown, ApplicantInterest | ''>(`${BASE}/${id}/interests`).then(r => r || null)

export interface ApplicantResidence {
  inChina: boolean
  country: string
  city: string | null
  address: string | null
  chinaEducationLevel: string | null
  chinaSchool: string | null
  visaType: string | null
  visaExpiryDate: string | null
}
export const getResidence = (id: number | string) =>
  request.get<unknown, ApplicantResidence | ''>(`${BASE}/${id}/residence`).then(r => r || null)

export interface ApplicantWork {
  id: number
  employer: string
  jobTitle: string
  employmentType: string | null
  country: string | null
  city: string | null
  startDate: string
  endDate: string | null
  current: boolean
  description: string | null
  workVisaType: string | null
  workVisaExpiry: string | null
}
export const listWork = (id: number | string) =>
  request.get<unknown, ApplicantWork[]>(`${BASE}/${id}/work`)

// ---- passport ----
export interface PassportMismatch { field: string, passportValue: string | null, profileValue: string | null }
export interface PassportStatus {
  /** masked unless the caller holds nad:applicant:pii:view */
  passportNo: string | null
  givenName: string | null
  familyName: string | null
  dob: string | null
  issueDate: string | null
  expiryDate: string | null
  readMethod: string | null
  edited: boolean
  scanUploaded: boolean
  validForAdmission: boolean
  matchesProfile: boolean
  mismatches: PassportMismatch[]
}
export const getPassportStatus = (id: number | string) =>
  request.get<unknown, PassportStatus>(`${BASE}/${id}/passport`)
/** Short-lived signed URL of the passport scan; absent (404) when no scan was uploaded. */
export const getPassportScanUrl = (id: number | string) =>
  request.get<unknown, ApplicantPhotoUrl>(`${BASE}/${id}/passport/scan`, { params: { json: 1 }, silent: true } as object)

