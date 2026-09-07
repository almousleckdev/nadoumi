import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '@/components/dashboard/StatCard.vue'
import { mountOpts } from '../helpers'

describe('StatCard', () => {
  it('formats a numeric value with locale grouping', () => {
    const w = mount(StatCard, { props: { label: 'Total applicants', value: 12345 }, ...mountOpts() })
    expect(w.find('.stat-card__value').text()).toBe(new Intl.NumberFormat().format(12345))
  })

  it('renders an empty value (no placeholder glyph) for a null value', () => {
    const w = mount(StatCard, { props: { label: 'Total', value: null }, ...mountOpts() })
    expect(w.find('.stat-card__value').text()).toBe('')
  })

  it('passes a string value through unchanged', () => {
    const w = mount(StatCard, { props: { label: 'Region', value: 'APAC' }, ...mountOpts() })
    expect(w.find('.stat-card__value').text()).toBe('APAC')
  })

  it('shows a skeleton and no value while loading', () => {
    const w = mount(StatCard, { props: { label: 'x', value: 1, state: 'loading' }, ...mountOpts() })
    expect(w.find('.stat-card__skeleton').exists()).toBe(true)
    expect(w.find('.stat-card__value').exists()).toBe(false)
  })

  it('shows the supplied error text in the error state', () => {
    const w = mount(StatCard, {
      props: { label: 'x', state: 'error', errorText: 'Backend unreachable' },
      ...mountOpts(),
    })
    expect(w.find('.stat-card__error').text()).toBe('Backend unreachable')
    expect(w.find('.stat-card__value').exists()).toBe(false)
  })

  it('falls back to the i18n error string when no errorText is given', () => {
    const w = mount(StatCard, { props: { label: 'x', state: 'error' }, ...mountOpts() })
    expect(w.find('.stat-card__error').text()).toBe('Could not load')
  })

  it('renders the hint only when provided', () => {
    const none = mount(StatCard, { props: { label: 'x', value: 1 }, ...mountOpts() })
    expect(none.find('.stat-card__hint').exists()).toBe(false)
    const some = mount(StatCard, { props: { label: 'x', value: 1, hint: 'Last 30 days' }, ...mountOpts() })
    expect(some.find('.stat-card__hint').text()).toBe('Last 30 days')
  })
})
