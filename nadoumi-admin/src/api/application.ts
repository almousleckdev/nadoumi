import request from '@/utils/request'
import type { Page } from './applicant'

/** Row of `GET /api/staff/applications` (`ApplicationResponse`). */
export interface Application {
  id: number
  applicantId: number
  applicationType: string | null
  programId: number | null
  scholarshipId: number | null
  intakeId: number | null
  currentStageCode: string | null
  currentStageName: string | null
  currentStatus: string | null
  assigneeUserId: number | null
  submittedAt: string | null
  /** Optimistic-lock version; a transition must send the version it read. */
  version: number
  createdAt: string | null
}

export interface ApplicationTask {
  id: number
  title: string
  roleRequired: string | null
  mandatory: boolean
  blocksExit: boolean
  status: string | null
  assigneeUserId: number | null
  dueAt: string | null
  skipReason: string | null
}

export interface StageHistory {
  id: number
  fromStageId: number | null
  toStageId: number | null
  transitionCode: string | null
  changedBy: number | null
  changedAt: string | null
  reason: string | null
}

export interface ApplicationEvent {
  id: number
  eventType: string
  actorUserId: number | null
  at: string | null
  detailJson: string | null
}

export interface ApplicationDecision {
  id: number
  decisionType: string
  outcome: string
  rationale: string
  decidedBy: number | null
  decidedAt: string | null
}

/** `GET /api/staff/applications/{id}` (`ApplicationDetailResponse`). */
export interface ApplicationDetail {
  application: Application
  tasks: ApplicationTask[]
  history: StageHistory[]
  events: ApplicationEvent[]
  decisions: ApplicationDecision[]
}

export interface ApplicationQuery {
  q?: string
  applicationType?: string
  status?: string
  stageId?: number
  assigneeUserId?: number
  page?: number
  size?: number
}

// perms: nad:application:list / :view / :transition / :decide / :assign / :claim
export const listApplications = (params: ApplicationQuery) =>
  request.get<unknown, Page<Application>>('/api/staff/applications', { params })

export const getApplication = (id: number) =>
  request.get<unknown, ApplicationDetail>(`/api/staff/applications/${id}`)

export const transitionApplication = (id: number, code: string, body: { reason?: string, version: number }) =>
  request.post<unknown, Application>(`/api/staff/applications/${id}/transitions/${encodeURIComponent(code)}`, body)

export const decideApplication = (id: number, body: { decisionType: string, outcome: string, rationale: string }) =>
  request.post<unknown, ApplicationDecision>(`/api/staff/applications/${id}/decisions`, body)

export const completeApplicationTask = (id: number, taskId: number) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/tasks/${taskId}/complete`)

export const skipApplicationTask = (id: number, taskId: number, reason: string) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/tasks/${taskId}/skip`, { reason })

export const assignApplication = (id: number, assigneeUserId: number) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/assign`, { assigneeUserId })

export const claimApplication = (id: number) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/claim`)
