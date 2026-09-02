import request from '@/utils/request'

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

export const archiveApplicant = (id: number | string) =>
  request.delete(`${BASE}/${id}`)

export const listEducation = (id: number | string) =>
  request.get<unknown, Education[]>(`${BASE}/${id}/education`)

export const listTestScores = (id: number | string) =>
  request.get<unknown, TestScore[]>(`${BASE}/${id}/test-scores`)

export const listContacts = (id: number | string) =>
  request.get<unknown, Contact[]>(`${BASE}/${id}/contacts`)
