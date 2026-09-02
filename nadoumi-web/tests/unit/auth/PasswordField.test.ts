import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PasswordField from '~/components/auth/PasswordField.vue'

describe('PasswordField', () => {
  it('starts masked and toggles reveal with an accessible button', async () => {
    const w = await mountSuspended(PasswordField, {
      props: { id: 'pw', modelValue: 'secret', label: 'Password' },
    })
    const input = w.find('#pw')
    expect(input.attributes('type')).toBe('password')

    const toggle = w.find('button[aria-pressed]')
    expect(toggle.attributes('aria-label')).toMatch(/show/i)
    await toggle.trigger('click')

    expect(w.find('#pw').attributes('type')).toBe('text')
    expect(w.find('button[aria-pressed]').attributes('aria-label')).toMatch(/hide/i)
  })

  it('emits update:modelValue on input', async () => {
    const w = await mountSuspended(PasswordField, {
      props: { id: 'pw', modelValue: '', label: 'Password' },
    })
    await w.find('#pw').setValue('abc')
    expect(w.emitted('update:modelValue')?.[0]).toEqual(['abc'])
  })

  it('shows the error and marks the input invalid', async () => {
    const w = await mountSuspended(PasswordField, {
      props: { id: 'pw', modelValue: 'x', label: 'Password', error: 'Passwords do not match.' },
    })
    expect(w.text()).toContain('Passwords do not match.')
    expect(w.find('#pw').attributes('aria-invalid')).toBe('true')
  })
})
