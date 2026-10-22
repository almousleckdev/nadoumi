import { describe, expect, it } from 'vitest'
import type { ChecklistItemDto } from '~/types/checklist'
import { allMandatoryAttached, isAttached, replacesExisting } from '~/utils/checklistView'

const item = (over: Partial<ChecklistItemDto>): ChecklistItemDto => ({
  docType: 'PASSPORT', mandatory: true, note: null, status: 'MISSING', documentId: null, rejectionReason: null, ...over,
})

describe('checklistView', () => {
  it('counts an uploaded document as attached, but not a missing or rejected one', () => {
    expect(isAttached(item({ status: 'SUBMITTED' }))).toBe(true)
    expect(isAttached(item({ status: 'IN_REVIEW' }))).toBe(true)
    expect(isAttached(item({ status: 'VERIFIED' }))).toBe(true)
    expect(isAttached(item({ status: 'MISSING' }))).toBe(false)
    expect(isAttached(item({ status: 'REJECTED' }))).toBe(false)
    expect(isAttached(item({ status: 'EXPIRED' }))).toBe(false)
  })

  it('is ready when every mandatory row is attached, whatever the optional rows say', () => {
    expect(allMandatoryAttached([])).toBe(true)
    expect(allMandatoryAttached([item({ status: 'SUBMITTED' }), item({ docType: 'CV', mandatory: false })])).toBe(true)
    expect(allMandatoryAttached([item({ status: 'SUBMITTED' }), item({ docType: 'TRANSCRIPT' })])).toBe(false)
  })

  it('replaces the existing document only when it was rejected or expired', () => {
    expect(replacesExisting(item({ status: 'REJECTED', documentId: 4 }))).toBe(true)
    expect(replacesExisting(item({ status: 'EXPIRED', documentId: 4 }))).toBe(true)
    expect(replacesExisting(item({ status: 'MISSING' }))).toBe(false)
    expect(replacesExisting(item({ status: 'SUBMITTED', documentId: 4 }))).toBe(false)
  })
})
