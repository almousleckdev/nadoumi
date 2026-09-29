import { describe, it, expect } from 'vitest'
import {
  eventNodeType, isOverdue, isTerminal, priorityTone, statusTone, transitionsFor,
} from '@/views/tasks/taskWorkflow'

const none = { progress: false, edit: false, approve: false }

describe('taskWorkflow', () => {
  it('maps priorities to badge tones', () => {
    expect(priorityTone('LOW')).toBe('PENDING')
    expect(priorityTone('MEDIUM')).toBe('SUBMITTED')
    expect(priorityTone('HIGH')).toBe('ACTIVE')
  })

  it('maps statuses to badge tones', () => {
    expect(statusTone('APPROVED')).toBe('ACTIVE')
    expect(statusTone('IN_PROGRESS')).toBe('IN_REVIEW')
    expect(statusTone('COMPLETED')).toBe('PENDING')
    expect(statusTone('CANCELLED')).toBe('FAILED')
    expect(statusTone('PENDING')).toBe('DRAFT')
  })

  it('treats approved and cancelled as terminal', () => {
    expect(isTerminal('APPROVED')).toBe(true)
    expect(isTerminal('CANCELLED')).toBe(true)
    expect(isTerminal('COMPLETED')).toBe(false)
  })

  it('marks progress steps as success and other events by kind', () => {
    expect(eventNodeType('STATUS_CHANGED')).toBe('success')
    expect(eventNodeType('CREATED')).toBe('success')
    expect(eventNodeType('ASSIGNED')).toBe('primary')
    expect(eventNodeType('PRIORITY_CHANGED')).toBe('warning')
    expect(eventNodeType('EDITED')).toBe('info')
  })

  it('flags only open tasks past their due date as overdue', () => {
    expect(isOverdue({ dueDate: '2000-01-01', status: 'PENDING' })).toBe(true)
    expect(isOverdue({ dueDate: '2000-01-01', status: 'APPROVED' })).toBe(false)
    expect(isOverdue({ dueDate: '2999-01-01', status: 'PENDING' })).toBe(false)
    expect(isOverdue({ dueDate: null, status: 'PENDING' })).toBe(false)
  })

  it('offers no transitions to a caller without progress or edit permission', () => {
    expect(transitionsFor('PENDING', none)).toEqual([])
  })

  it('offers the forward moves from each open status', () => {
    const p = { ...none, progress: true }
    expect(transitionsFor('PENDING', p).map(t => t.to)).toEqual(['IN_PROGRESS', 'CANCELLED'])
    expect(transitionsFor('IN_PROGRESS', p).map(t => t.to)).toEqual(['COMPLETED', 'PENDING', 'CANCELLED'])
    expect(transitionsFor('COMPLETED', p).map(t => t.to)).toEqual(['IN_PROGRESS', 'CANCELLED'])
  })

  it('adds approval to a completed task only for approvers', () => {
    expect(transitionsFor('COMPLETED', { ...none, edit: true, approve: true }).map(t => t.to))
      .toEqual(['APPROVED', 'IN_PROGRESS', 'CANCELLED'])
  })

  it('offers nothing from a terminal status', () => {
    expect(transitionsFor('APPROVED', { progress: true, edit: true, approve: true })).toEqual([])
    expect(transitionsFor('CANCELLED', { progress: true, edit: true, approve: true })).toEqual([])
  })
})
