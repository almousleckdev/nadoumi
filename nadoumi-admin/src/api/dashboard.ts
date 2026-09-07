import request from '@/utils/request'
import type { ApplicantRow, Page } from './applicant'

export function listApplicantsPage(params: {
  status?: string
  nationality?: string
  createdAfter?: string
  page?: number
  size?: number
}) {
  return request.get<unknown, Page<ApplicantRow>>('/api/staff/applicants', { params })
}
