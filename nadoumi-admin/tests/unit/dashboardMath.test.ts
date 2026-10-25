import { describe, it, expect } from 'vitest'
import { openTasks } from '@/views/dashboard/dashboardMath'
import type { Task } from '@/api/hr'

const task = (status: Task['status']) => ({ id: 1, title: 't', status }) as unknown as Task

describe('openTasks', () => {
  it('keeps pending and in-progress tasks and drops finished ones', () => {
    const tasks = [task('PENDING'), task('IN_PROGRESS'), task('COMPLETED'), task('APPROVED'), task('CANCELLED')]
    expect(openTasks(tasks).map(t => t.status)).toEqual(['PENDING', 'IN_PROGRESS'])
  })
})
