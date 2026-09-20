import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OnboardingProgress from '~/components/onboarding/OnboardingProgress.vue'

const steps = [
  { key: 'a', label: 'Personal' },
  { key: 'b', label: 'Identity' },
  { key: 'c', label: 'Review' },
]

describe('OnboardingProgress', () => {
  it('marks the current segment with aria-current and shows its label', async () => {
    const w = await mountSuspended(OnboardingProgress, { props: { steps, current: 1 } })
    const segments = w.findAll('ol li span')
    expect(segments[1]!.attributes('aria-current')).toBe('step')
    expect(segments[0]!.attributes('aria-current')).toBeUndefined()
    expect(segments[2]!.attributes('aria-current')).toBeUndefined()
    expect(w.text()).toContain('Identity')
  })

  it('fills segments up to and including the current step, not beyond', async () => {
    const w = await mountSuspended(OnboardingProgress, { props: { steps, current: 1 } })
    const segments = w.findAll('ol li span')
    expect(segments[0]!.classes()).toContain('w-full') // done
    expect(segments[1]!.classes()).toContain('w-full') // current
    expect(segments[2]!.classes()).toContain('w-0') // upcoming
  })
})
