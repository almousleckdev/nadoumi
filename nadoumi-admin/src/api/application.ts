import request from '@/utils/request'
import type { Page } from './applicant'

/** Row of `GET /api/staff/applications`: the application with the names a case officer reads. */
export interface ApplicationRow {
  id: number
  reference: string | null
  applicantId: number
  applicantName: string
  applicationType: string | null
  opportunityTitle: string | null
  universityName: string | null
  intake: string | null
  currentStageName: string | null
  currentStatus: string | null
  assigneeUserId: number | null
  assigneeName: string | null
  submittedAt: string | null
}

/** `ApplicationResponse`: the application itself, as returned inside the detail and after a change. */
export interface Application {
  id: number
  /** The readable application number, for example NAD20261023001. */
  reference: string | null
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
  fromStageName: string | null
  toStageName: string | null
  changedByName: string | null
}

export interface ApplicationEvent {
  id: number
  eventType: string
  actorUserId: number | null
  at: string | null
  detailJson: string | null
  actorName: string | null
  /** The detail in words (an assignee id becomes their name). */
  detailText: string | null
}

export interface ApplicationDecision {
  id: number
  decisionType: string
  outcome: string
  rationale: string
  decidedBy: number | null
  decidedAt: string | null
  decidedByName: string | null
}

/** `GET /api/staff/applications/{id}` (`ApplicationDetailResponse`). */
export interface ApplicationDetail {
  application: Application
  tasks: ApplicationTask[]
  history: StageHistory[]
  events: ApplicationEvent[]
  decisions: ApplicationDecision[]
  applicant: { id: number, publicId: string, name: string, email: string | null, phone: string | null, nationality: string | null }
  opportunity: { kind: string, title: string | null, universityName: string | null, intake: string | null }
  assigneeName: string | null
}

/** A status change available from the current stage. */
export interface NextStep {
  code: string
  label: string
  status: string
  /** The formal decision the step records (for example "Eligible"), or null. */
  decision: string | null
  /** Why it cannot be taken right now, or null when it can. */
  blockedReason: string | null
}

export interface StaffUser {
  userId: number
  name: string
  userName: string
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
  request.get<unknown, Page<ApplicationRow>>('/api/staff/applications', { params })

export const getApplication = (id: number) =>
  request.get<unknown, ApplicationDetail>(`/api/staff/applications/${id}`)

export const listNextSteps = (id: number) =>
  request.get<unknown, NextStep[]>(`/api/staff/applications/${id}/next-steps`)

/** Takes one status change; the server records the decision it stands for and e-mails the student. */
export const advanceApplication = (id: number, body: { code: string, note?: string, version: number }) =>
  request.post<unknown, Application>(`/api/staff/applications/${id}/advance`, body)

export const listAssignees = () =>
  request.get<unknown, StaffUser[]>('/api/staff/applications/assignees')

/** The submitted application form (PDF), as a Blob. */
export const fetchApplicationForm = (id: number) =>
  request.get<unknown, Blob>(`/api/staff/applications/${id}/form`, { responseType: 'blob' })

export const completeApplicationTask = (id: number, taskId: number) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/tasks/${taskId}/complete`)

export const skipApplicationTask = (id: number, taskId: number, reason: string) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/tasks/${taskId}/skip`, { reason })

export const assignApplication = (id: number, assigneeUserId: number) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/assign`, { assigneeUserId })

export const claimApplication = (id: number) =>
  request.post<unknown, void>(`/api/staff/applications/${id}/claim`)

/** Permanent; the server refuses while a confirmed payment exists. */
export const deleteApplication = (id: number, reason: string) =>
  request.delete<unknown, void>(`/api/staff/applications/${id}`, { data: { reason } })
