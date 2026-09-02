import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DonutStat from '@/components/dashboard/DonutStat.vue'
import { mountOpts } from '../helpers'

const segments = [
  { label: 'Complete', value: 30, color: '#f97316' },
  { label: 'Incomplete', value: 10, color: '#e5e7eb' },
]

describe('DonutStat', () => {
  it('draws one arc per segment and a legend row per segment', () => {
    const w = mount(DonutStat, { props: { label: 'Profile completion', segments }, ...mountOpts() })
    expect(w.findAll('.donut__arc')).toHaveLength(2)
    expect(w.findAll('.donut__legend li')).toHaveLength(2)
  })

  it('arc lengths are proportional to the values and sum to 100', () => {
    const w = mount(DonutStat, { props: { label: 'x', segments }, ...mountOpts() })
    const arcs = w.findAll('.donut__arc')
    const lens = arcs.map(a => Number(a.attributes('stroke-dasharray')!.split(' ')[0]))
    expect(lens[0]).toBeCloseTo(75)
    expect(lens[1]).toBeCloseTo(25)
  })

  it('shows the provided center value and an accessible summary', () => {
    const w = mount(DonutStat, {
      props: { label: 'Profile completion', segments, centerValue: '75%' },
      ...mountOpts(),
    })
    expect(w.find('.donut__center-num').text()).toBe('75%')
    expect(w.find('svg').attributes('aria-label')).toContain('Complete 30')
  })

  it('renders a skeleton while loading and an error message on error', () => {
    const l = mount(DonutStat, { props: { label: 'x', segments, state: 'loading' }, ...mountOpts() })
    expect(l.find('.donut__skeleton').exists()).toBe(true)
    expect(l.find('.donut__body').exists()).toBe(false)

    const e = mount(DonutStat, {
      props: { label: 'x', segments, state: 'error', errorText: 'boom' },
      ...mountOpts(),
    })
    expect(e.find('.donut__error').text()).toBe('boom')
  })
})
