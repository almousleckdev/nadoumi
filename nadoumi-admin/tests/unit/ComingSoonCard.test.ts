import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ComingSoonCard from '@/components/dashboard/ComingSoonCard.vue'
import { mountOpts } from '../helpers'

describe('ComingSoonCard', () => {
  it('shows the label, a not-yet-available tag and an em-dash value (never a number)', () => {
    const w = mount(ComingSoonCard, { props: { label: 'Revenue', domain: 'Finance' }, ...mountOpts() })
    expect(w.find('.cs-card__label').text()).toBe('Revenue')
    expect(w.text()).toContain('Not yet available')
    expect(w.find('.cs-card__value').text()).toBe('—')
  })

  it('interpolates the domain into the default note', () => {
    const w = mount(ComingSoonCard, { props: { label: 'Net earnings', domain: 'Finance' }, ...mountOpts() })
    expect(w.find('.cs-card__note').text()).toContain('Finance module')
  })

  it('prefers an explicit note over the default', () => {
    const w = mount(ComingSoonCard, {
      props: { label: 'x', domain: 'Workflow', note: 'Blocked on workflow engine' },
      ...mountOpts(),
    })
    expect(w.find('.cs-card__note').text()).toBe('Blocked on workflow engine')
  })
})
