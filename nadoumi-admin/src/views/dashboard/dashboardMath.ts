import type { Task } from '@/api/hr'

const OPEN_TASK_STATUSES = new Set(['PENDING', 'IN_PROGRESS'])

export function openTasks(tasks: Task[]): Task[] {
  return tasks.filter(task => OPEN_TASK_STATUSES.has(task.status))
}
