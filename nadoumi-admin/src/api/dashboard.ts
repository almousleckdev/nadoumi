import request from '@/utils/request'
import type { ApplicantRow, Page } from './applicant'

/**
 * Interim dashboard data source. The approved target (docs/ADMIN_ARCHITECTURE.md §6)
 * is the Reporting slice's `rm_*` read models; until those exist, the Applicant
 * tiles read the operational staff endpoint directly. Every other domain
 * (Applications, Finance, Payments, Employees, Workflow, Activity) is not built
 * yet and is shown as a coming-soon state — never fabricated.
 */
export function listApplicantsPage(params: {
  status?: string
  nationality?: string
  createdAfter?: string
  page?: number
  size?: number
}) {
  return request.get<unknown, Page<ApplicantRow>>('/api/staff/applicants', { params })
}
