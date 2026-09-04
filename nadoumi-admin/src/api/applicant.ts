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
}

/** Back-compat alias used by list views. */
export type ApplicantRow = Applicant

export interface Education {
  id: number
  institution: string
  level: string | null
  field: string | null
  gpa: number | null
  gpaScale: number | null
  startDate: string | null
  endDate: string | null
}

export interface TestScore {
  id: number
  testType: string
  score: string
  subScoresJson: string | null
  takenOn: string | null
  expiresOn: string | null
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

export interface TestScoreInput {
  testType: string
  score: string
  subScoresJson?: string | null
  takenOn?: string | null
  expiresOn?: string | null
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

export const archiveApplicant = (id: number | string) =>
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

// ---- test scores ----
export const listTestScores = (id: number | string) =>
  request.get<unknown, TestScore[]>(`${BASE}/${id}/test-scores`)
export const addTestScore = (id: number | string, body: TestScoreInput) =>
  request.post<unknown, TestScore>(`${BASE}/${id}/test-scores`, body)
export const updateTestScore = (id: number | string, scoreId: number, body: TestScoreInput) =>
  request.put<unknown, TestScore>(`${BASE}/${id}/test-scores/${scoreId}`, body)
export const deleteTestScore = (id: number | string, scoreId: number) =>
  request.delete(`${BASE}/${id}/test-scores/${scoreId}`)

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
