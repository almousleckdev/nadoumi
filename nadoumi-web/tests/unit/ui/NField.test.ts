import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NField from '~/components/ui/NField.vue'

describe('NField', () => {
  it('renders the label bound to the control id', async () => {
    const w = await mountSuspended(NField, { props: { label: 'Email', for: 'email' } })
    const label = w.find('label')
    expect(label.text()).toContain('Email')
    expect(label.attributes('for')).toBe('email')
  })

  it('shows a required marker when required', async () => {
    const w = await mountSuspended(NField, { props: { label: 'Email', for: 'email', required: true } })
    expect(w.text()).toContain('*')
  })

  it('renders the hint text', async () => {
    const w = await mountSuspended(NField, { props: { label: 'X', for: 'x', hint: 'help me' } })
    expect(w.find('#x-hint').text()).toBe('help me')
  })

  it('renders an alert error with the id the control points to', async () => {
    const w = await mountSuspended(NField, { props: { label: 'X', for: 'x', error: 'is required' } })
    const err = w.find('#x-error')
    expect(err.exists()).toBe(true)
    expect(err.attributes('role')).toBe('alert')
    expect(err.text()).toBe('is required')
  })

  it('hides the error node when there is no error', async () => {
    const w = await mountSuspended(NField, { props: { label: 'X', for: 'x' } })
    expect(w.find('#x-error').exists()).toBe(false)
  })
})
