import { describe, it, expect } from 'vitest'
import { statusChangeCopy, statusLabelKey, statusTone } from '@/views/applicants/studentStatus'

describe('statusTone', () => {
  it('maps active, suspended and blocked accounts to badge tones', () => {
    expect(statusTone('0')).toBe('ACTIVE')
    expect(statusTone('1')).toBe('PENDING')
    expect(statusTone('2')).toBe('FAILED')
  })

  it('treats an unknown status as blocked', () => {
    expect(statusTone('9')).toBe('FAILED')
  })
})

describe('statusLabelKey', () => {
  it('names the translation of each account status', () => {
    expect(statusLabelKey('0')).toBe('students.statusActive')
    expect(statusLabelKey('1')).toBe('students.statusSuspended')
    expect(statusLabelKey('2')).toBe('students.statusBlocked')
  })
})

describe('statusChangeCopy', () => {
  it('describes suspending, blocking and reactivating', () => {
    expect(statusChangeCopy('1')).toEqual({ title: 'students.suspend', confirm: 'students.suspendConfirm', tone: 'warning' })
    expect(statusChangeCopy('2')).toEqual({ title: 'students.block', confirm: 'students.blockConfirm', tone: 'warning' })
    expect(statusChangeCopy('0')).toEqual({ title: 'students.unblock', confirm: 'students.unblockConfirm', tone: 'info' })
  })
})
