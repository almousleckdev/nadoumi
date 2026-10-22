import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OnboardingStepper from '~/components/onboarding/OnboardingStepper.vue'

const steps = [
  { key: 'a', label: 'Personal', done: true },
  { key: 'b', label: 'Identity', done: false },
  { key: 'c', label: 'Work', optional: true, done: null },
  { key: 'd', label: 'Review', done: null },
]

describe('OnboardingStepper', () => {
  it('marks the current step, the finished ones and the rest', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 1 } })
    const states = w.findAll('[data-test="step"]').map(s => s.attributes('data-state'))
    expect(states).toEqual(['done', 'current', 'upcoming', 'upcoming'])
    expect(w.find('[data-test="step"][aria-current="step"]').text()).toContain('Identity')
  })

  it('counts progress over the steps the server tracks only', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 0 } })
    expect(w.find('[data-test="percent"]').text()).toBe('50%')
  })

  it('tags an optional step', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 0 } })
    expect(w.findAll('[data-test="step"]')[2]!.text()).toContain('Optional')
  })

  it('lets the student open any step from the sidebar', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 0 } })
    const buttons = w.findAll('[data-test="step"]')
    expect(buttons.every(b => b.attributes('disabled') === undefined)).toBe(true)

    await buttons[3]!.trigger('click')
    expect(w.emitted('select')).toEqual([[3]])
  })

  it('uses the same sidebar colours as the admin: brand for the active row only', async () => {
    const w = await mountSuspended(OnboardingStepper, { props: { steps, current: 1 } })
    const rows = w.findAll('[data-test="step"]')
    expect(rows[1]!.classes()).toContain('bg-brand-600')
    expect(rows[0]!.classes()).not.toContain('bg-brand-600')
  })
})
