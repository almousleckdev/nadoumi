import request from '@/utils/request'

export interface ApplicantRow {
  id: number
  givenName: string
  familyName: string
  dob: string | null
  nationality: string | null
  passportNo: string | null
  email: string | null
  phone: string | null
  status: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const listApplicants = (params: {
  name?: string
  status?: string
  nationality?: string
  page?: number
  size?: number
}) => request.get<any, Page<ApplicantRow>>('/api/staff/applicants', { params })

export const createApplicant = (body: {
  givenName: string
  familyName: string
  invitedEmail: string
  dob?: string
  nationality?: string
  email?: string
  phone?: string
}) => request.post<any, ApplicantRow>('/api/staff/applicants', body)

export const archiveApplicant = (id: number) => request.delete(`/api/staff/applicants/${id}`)
