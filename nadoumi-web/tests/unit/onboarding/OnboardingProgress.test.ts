import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OnboardingProgress from '~/components/onboarding/OnboardingProgress.vue'

const steps = [
  { key: 'a', label: 'Personal' },
  { key: 'b', label: 'Identity' },
  { key: 'c', label: 'Review' },
]

describe('OnboardingProgress', () => {
  it('marks completed steps done and the current step with aria-current', async () => {
    const w = await mountSuspended(OnboardingProgress, { props: { steps, current: 1 } })
    const markers = w.findAll('ol li span:first-child')
    expect(markers[0]!.text()).toBe('✓') // done
    expect(markers[1]!.attributes('aria-current')).toBe('step')
    expect(markers[2]!.text()).toBe('3') // upcoming
  })
})
