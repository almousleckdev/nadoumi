import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OnboardingProgress from '~/components/onboarding/OnboardingProgress.vue'

const steps = [
  { key: 'a', label: 'Personal', done: true },
  { key: 'b', label: 'Identity', done: false },
  { key: 'c', label: 'Review', done: null },
]

describe('OnboardingProgress', () => {
  it('marks the current segment with aria-current and shows its label', async () => {
    const w = await mountSuspended(OnboardingProgress, { props: { steps, current: 1 } })
    const segments = w.findAll('ol li button')
    expect(segments[1]!.attributes('aria-current')).toBe('step')
    expect(segments[0]!.attributes('aria-current')).toBeUndefined()
    expect(w.text()).toContain('Identity')
  })

  it('fills the finished and the current segments, not the rest', async () => {
    const w = await mountSuspended(OnboardingProgress, { props: { steps, current: 1 } })
    const bars = w.findAll('ol li button span')
    expect(bars[0]!.classes()).toContain('bg-brand-600')
    expect(bars[1]!.classes()).toContain('bg-brand-600')
    expect(bars[2]!.classes()).toContain('bg-slate-200')
  })

  it('opens a step when its segment is tapped', async () => {
    const w = await mountSuspended(OnboardingProgress, { props: { steps, current: 0 } })
    await w.findAll('ol li button')[2]!.trigger('click')
    expect(w.emitted('select')).toEqual([[2]])
  })
})
