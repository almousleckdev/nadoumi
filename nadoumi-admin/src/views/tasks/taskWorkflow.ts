import type { Task } from '@/api/hr'

export const TASK_STATUSES = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'APPROVED', 'CANCELLED']
export const TASK_PRIORITIES = ['HIGH', 'MEDIUM', 'LOW']

export interface TaskTransition {
  to: string
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

export interface TransitionAccess {
  progress: boolean
  edit: boolean
  approve: boolean
}

export function priorityTone(p: string): string {
  return p === 'LOW' ? 'PENDING' : p === 'MEDIUM' ? 'SUBMITTED' : 'ACTIVE'
}

export function statusTone(s: string): string {
  return s === 'APPROVED' ? 'ACTIVE' : s === 'IN_PROGRESS' ? 'IN_REVIEW'
    : s === 'COMPLETED' ? 'PENDING' : s === 'CANCELLED' ? 'FAILED' : 'DRAFT'
}

export function isTerminal(s: string) {
  return s === 'APPROVED' || s === 'CANCELLED'
}

export function eventNodeType(type: string): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'STATUS_CHANGED' || type === 'CREATED') return 'success'
  if (type === 'ASSIGNED') return 'primary'
  if (type === 'PRIORITY_CHANGED') return 'warning'
  return 'info'
}

export function isOverdue(task: Pick<Task, 'dueDate' | 'status'>) {
  return task.dueDate != null && !isTerminal(task.status) && task.dueDate < new Date().toISOString().slice(0, 10)
}

export function transitionsFor(status: string, access: TransitionAccess): TaskTransition[] {
  if (!access.progress && !access.edit) return []
  switch (status) {
    case 'PENDING': return [{ to: 'IN_PROGRESS', type: 'primary' }, { to: 'CANCELLED', type: 'danger' }]
    case 'IN_PROGRESS': return [{ to: 'COMPLETED', type: 'success' }, { to: 'PENDING', type: 'info' }, { to: 'CANCELLED', type: 'danger' }]
    case 'COMPLETED': {
      const moves: TaskTransition[] = [{ to: 'IN_PROGRESS', type: 'warning' }, { to: 'CANCELLED', type: 'danger' }]
      if (access.approve) moves.unshift({ to: 'APPROVED', type: 'success' })
      return moves
    }
    default: return []
  }
}
