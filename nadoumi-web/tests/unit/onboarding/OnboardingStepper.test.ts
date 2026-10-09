import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OnboardingStepper from '~/components/onboarding/OnboardingStepper.vue'

const steps = [
  { key: 'a', label: 'Personal' },
  { key: 'b', label: 'Identity' },
  { key: 'c', label: 'Work', optional: true },
  { key: 'd', label: 'Review' },
]

describe('OnboardingStepper', () => {
  it('marks finished, current and upcoming steps and shows the percentage', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 1, reached: 1 } })
    const states = w.findAll('[data-test="step"]').map(s => s.attributes('data-state'))
    expect(states).toEqual(['done', 'current', 'upcoming', 'upcoming'])
    expect(w.find('[data-test="step"][aria-current="step"]').text()).toContain('Identity')
    expect(w.find('[data-test="percent"]').text()).toBe('33%')
  })

  it('tags an optional step', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 0, reached: 0 } })
    expect(w.findAll('[data-test="step"]')[2]!.text()).toContain('Optional')
  })

  it('lets the student go back to a finished step but not jump ahead', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 2, reached: 2 } })
    const buttons = w.findAll('[data-test="step"]')
    expect(buttons[0]!.attributes('disabled')).toBeUndefined()
    expect(buttons[3]!.attributes('disabled')).toBeDefined() // beyond the furthest step reached
    expect(buttons[2]!.attributes('disabled')).toBeDefined() // already here

    await buttons[0]!.trigger('click')
    expect(w.emitted('select')).toEqual([[0]])
  })

  it('lets the student return to a step they already reached after going back', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 0, reached: 2 } })
    expect(w.findAll('[data-test="step"]')[2]!.attributes('disabled')).toBeUndefined()
  })
})
