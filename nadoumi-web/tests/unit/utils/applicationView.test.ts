import { describe, it, expect } from 'vitest'
import { isClosed, safeTimeline, statusKey, statusTone } from '~/utils/applicationView'

describe('safeTimeline', () => {
  it('maps known milestones and collapses other stage changes to one neutral label', () => {
    expect(safeTimeline(['APPLICATION_CREATED', 'STAGE_SUBMIT', 'STAGE_ELIGIBLE', 'STAGE_DOCS_COMPLETE', 'STAGE_ACCEPT_OFFER']))
      .toEqual(['created', 'submitted', 'stageChanged', 'stageChanged', 'offerAccepted'])
  })

  it('drops internal events and never renders unknown codes', () => {
    expect(safeTimeline(['CASE_ASSIGNED', 'TASK_COMPLETED', 'TASK_SKIPPED', 'SOMETHING_NEW'])).toEqual([])
  })
})

describe('status helpers', () => {
  it('falls back to a generic label for statuses the UI has no copy for', () => {
    expect(statusKey('IN_REVIEW')).toBe('IN_REVIEW')
    expect(statusKey('INTERNAL_QUEUE')).toBe('unknown')
    expect(statusKey(null)).toBe('unknown')
  })

  it('picks a tone per outcome and detects closed applications', () => {
    expect(statusTone('CLOSED_SUCCESS')).toBe('success')
    expect(statusTone('CLOSED_UNSUCCESSFUL')).toBe('danger')
    expect(statusTone('DRAFT')).toBe('warning')
    expect(isClosed('CLOSED_WITHDRAWN')).toBe(true)
    expect(isClosed('IN_REVIEW')).toBe(false)
  })
})
