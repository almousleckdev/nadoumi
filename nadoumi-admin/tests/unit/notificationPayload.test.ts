import { describe, it, expect } from 'vitest'
import { deliveryTone, parsePayload, payloadRows, taskIdOf } from '@/views/notifications/notificationPayload'

describe('parsePayload', () => {
  it('parses a JSON object', () => {
    expect(parsePayload('{"a":1}')).toEqual({ a: 1 })
  })

  it('returns an empty object for missing, blank or invalid input', () => {
    expect(parsePayload(null)).toEqual({})
    expect(parsePayload(undefined)).toEqual({})
    expect(parsePayload('')).toEqual({})
    expect(parsePayload('{not json')).toEqual({})
  })

  it('returns an empty object when the JSON is not an object', () => {
    expect(parsePayload('[1,2]')).toEqual({})
    expect(parsePayload('"text"')).toEqual({})
    expect(parsePayload('42')).toEqual({})
  })
})

describe('payloadRows', () => {
  it('lists each populated key as text and hides recipients, blanks and nulls', () => {
    expect(payloadRows({ email: 'a@x.com', count: 3, recipientUserIds: [1], empty: '', nothing: null })).toEqual([
      { k: 'email', v: 'a@x.com' },
      { k: 'count', v: '3' },
    ])
  })
})

describe('taskIdOf', () => {
  it('reads a numeric task id', () => {
    expect(taskIdOf({ taskId: 33 })).toBe(33)
    expect(taskIdOf({ taskId: '34' })).toBe(34)
  })

  it('is null when there is no usable task id', () => {
    expect(taskIdOf({})).toBeNull()
    expect(taskIdOf({ taskId: 'abc' })).toBeNull()
    expect(taskIdOf({ taskId: 0 })).toBeNull()
  })
})

describe('deliveryTone', () => {
  it('maps delivery states to a badge tone', () => {
    expect(deliveryTone('SENT')).toBe('ACTIVE')
    expect(deliveryTone('DELIVERED')).toBe('ACTIVE')
    expect(deliveryTone('FAILED')).toBe('FAILED')
    expect(deliveryTone('BOUNCED')).toBe('FAILED')
    expect(deliveryTone('QUEUED')).toBe('PENDING')
  })
})
