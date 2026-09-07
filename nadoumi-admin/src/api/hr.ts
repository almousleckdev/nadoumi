import request from '@/utils/request'
import type { Page } from './applicant'

// ---- employees
export interface Employee {
  id: number
  userId: number
  employeeNo: string | null
  userName: string
  nickName: string
  email: string | null
  phone: string | null
  userStatus: string
  positionId: number | null
  positionTitle: string | null
  deptId: number | null
  deptName: string | null
  managerUserId: number | null
  managerName: string | null
  employmentType: string
  employmentStatus: string
  startDate: string
  probationEndDate: string | null
  endDate: string | null
  workLocation: string | null
  salaryAmount: number | null
  salaryCurrency: string | null
  payFrequency: string
  compensationVisible: boolean
  emergencyContact: string | null
  emergencyContactRelationship: string | null
  emergencyContactPhone: string | null
  emergencyContactEmail: string | null
  notes: string | null
  createTime: string | null
  /** Role ids currently granted to this employee's staff account. */
  roleIds: number[]
}

export interface EmployeeInput {
  userName?: string
  password?: string
  nickName: string
  email?: string | null
  phone?: string | null
  userStatus?: string
  roleIds?: number[]
  positionId?: number | null
  positionTitle?: string | null
  deptId?: number | null
  managerUserId?: number | null
  employmentType: string
  employmentStatus: string
  startDate: string
  probationEndDate?: string | null
  endDate?: string | null
  workLocation?: string | null
  salaryAmount?: number | null
  salaryCurrency?: string | null
  payFrequency?: string
  emergencyContact?: string | null
  emergencyContactRelationship?: string | null
  emergencyContactPhone?: string | null
  emergencyContactEmail?: string | null
  notes?: string | null
}

const EMP = '/api/staff/employees'
export const listEmployees = (params: {
  q?: string, deptId?: number, employmentStatus?: string, page?: number, size?: number
}) => request.get<unknown, Page<Employee>>(EMP, { params })
export const getEmployee = (id: number) => request.get<unknown, Employee>(`${EMP}/${id}`)
export const createEmployee = (body: EmployeeInput) => request.post<unknown, Employee>(EMP, body)
export const updateEmployee = (id: number, body: EmployeeInput) => request.put<unknown, Employee>(`${EMP}/${id}`, body)
export const deleteEmployee = (id: number) => request.delete(`${EMP}/${id}`)

// tasks
export interface TaskEvent {
  id: number
  eventType: string
  fromStatus: string | null
  toStatus: string | null
  actorName: string | null
  note: string | null
  createdAt: string
}

export interface Task {
  id: number
  title: string
  description: string | null
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'APPROVED' | 'CANCELLED'
  assigneeUserId: number | null
  assigneeName: string | null
  createdByUserId: number
  createdByName: string | null
  dueDate: string | null
  startedAt: string | null
  completedAt: string | null
  approvedByName: string | null
  approvedAt: string | null
  relatedType: string | null
  relatedId: number | null
  createTime: string | null
  events: TaskEvent[]
}

export interface TaskInput {
  title: string
  description?: string | null
  priority: string
  assigneeUserId?: number | null
  dueDate?: string | null
  relatedType?: string | null
  relatedId?: number | null
}

const TASK = '/api/staff/tasks'
export const listTasks = (params: {
  q?: string, status?: string, priority?: string, assigneeUserId?: number, mine?: boolean,
  page?: number, size?: number
}) => request.get<unknown, Page<Task>>(TASK, { params })
export const getTask = (id: number) => request.get<unknown, Task>(`${TASK}/${id}`)
export const createTask = (body: TaskInput) => request.post<unknown, Task>(TASK, body)
export const updateTask = (id: number, body: TaskInput) => request.put<unknown, Task>(`${TASK}/${id}`, body)
export const changeTaskStatus = (id: number, status: string, note?: string) =>
  request.put<unknown, Task>(`${TASK}/${id}/status`, { status, note })
export const deleteTask = (id: number) => request.delete(`${TASK}/${id}`)

// ---- payroll (read-only compensation roll-up) ----
export interface PayrollCurrencyLine {
  currency: string
  monthly: number
  annual: number
  headcount: number
}
export interface PayrollRow {
  employeeId: number
  name: string
  deptName: string | null
  positionTitle: string | null
  employmentStatus: string
  salaryAmount: number
  salaryCurrency: string
  payFrequency: string
  monthlyEquivalent: number
}
export interface PayrollSummary {
  byCurrency: PayrollCurrencyLine[]
  rows: PayrollRow[]
}
export const getPayroll = () => request.get<unknown, PayrollSummary>('/api/staff/payroll')
