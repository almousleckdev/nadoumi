import request from '@/utils/request'

/** Head counts keyed by id; an id with nobody behind it is absent, so read a miss as zero. */
export interface OrgOverview {
  roles: Record<string, number>
  departments: Record<string, number>
  posts: Record<string, number>
}

export const getOrgOverview = () => request.get<unknown, OrgOverview>('/api/staff/org/overview')
