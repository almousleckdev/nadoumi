import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NButton from '~/components/ui/NButton.vue'

describe('NButton', () => {
  it('renders a button with the label', async () => {
    const w = await mountSuspended(NButton, { slots: { default: () => 'Save' } })
    expect(w.find('button').exists()).toBe(true)
    expect(w.text()).toContain('Save')
  })

  it('applies the primary variant classes by default', async () => {
    const w = await mountSuspended(NButton, { slots: { default: () => 'Go' } })
    expect(w.find('button').classes().join(' ')).toContain('bg-brand-600')
  })

  it('renders a NuxtLink when "to" is set', async () => {
    const w = await mountSuspended(NButton, { props: { to: '/x' }, slots: { default: () => 'Link' } })
    expect(w.find('a').exists()).toBe(true)
    expect(w.find('button').exists()).toBe(false)
  })

  it('is disabled and shows the spinner while loading', async () => {
    const w = await mountSuspended(NButton, { props: { loading: true }, slots: { default: () => 'X' } })
    expect(w.find('button').attributes('disabled')).toBeDefined()
    expect(w.find('.n-btn__spin').exists()).toBe(true)
  })

  it('sets aria-disabled and blocks the link role when disabled with "to"', async () => {
    const w = await mountSuspended(NButton, { props: { to: '/x', disabled: true }, slots: { default: () => 'X' } })
    expect(w.find('a').attributes('aria-disabled')).toBe('true')
  })
})
