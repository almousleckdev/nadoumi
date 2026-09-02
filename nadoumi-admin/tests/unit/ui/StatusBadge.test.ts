import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from '@/components/ui/StatusBadge.vue'

describe('StatusBadge', () => {
  it('maps known statuses to the shared tone palette', () => {
    expect(mount(StatusBadge, { props: { status: 'ACTIVE' } }).classes()).toContain('badge--success')
    expect(mount(StatusBadge, { props: { status: 'REJECTED' } }).classes()).toContain('badge--danger')
    expect(mount(StatusBadge, { props: { status: 'PENDING_REVIEW' } }).classes()).toContain('badge--warning')
    expect(mount(StatusBadge, { props: { status: 'DRAFT' } }).classes()).toContain('badge--neutral')
  })

  it('falls back to neutral for an unknown status', () => {
    expect(mount(StatusBadge, { props: { status: 'WOBBLE' } }).classes()).toContain('badge--neutral')
  })

  it('title-cases the status for the label and honours an explicit label', () => {
    expect(mount(StatusBadge, { props: { status: 'PENDING_REVIEW' } }).text()).toBe('Pending review')
    expect(mount(StatusBadge, { props: { status: 'X', label: 'Custom' } }).text()).toBe('Custom')
  })

  it('accepts a per-call override map', () => {
    const w = mount(StatusBadge, { props: { status: 'DRAFT', map: { DRAFT: 'danger' } } })
    expect(w.classes()).toContain('badge--danger')
  })
})
