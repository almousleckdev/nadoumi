import { describe, it, expect } from 'vitest'
import { problemMessage } from '~/composables/useApi'

describe('problemMessage', () => {
  it('prefers problem.detail', () => {
    const err = { data: { detail: 'username must be at least 2', title: 'Bad Request' }, message: 'fail' }
    expect(problemMessage(err, 'x')).toBe('username must be at least 2')
  })

  it('falls back to title, then message, then the fallback', () => {
    expect(problemMessage({ data: { title: 'Conflict' }, message: 'm' }, 'x')).toBe('Conflict')
    expect(problemMessage({ message: 'boom' }, 'x')).toBe('boom')
    expect(problemMessage({}, 'the fallback')).toBe('the fallback')
  })
})
