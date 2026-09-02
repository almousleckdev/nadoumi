import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PasswordRequirements from '~/components/auth/PasswordRequirements.vue'

describe('PasswordRequirements', () => {
  it('lists the rules while the password is weak', async () => {
    const w = await mountSuspended(PasswordRequirements, { props: { value: 'abc' } })
    expect(w.text()).toContain('characters')
    expect(w.text()).not.toContain('Strong password')
  })

  it('shows the strong state once every rule passes', async () => {
    const w = await mountSuspended(PasswordRequirements, { props: { value: 'Abcdef1!' } })
    expect(w.text()).toContain('Strong password')
  })

  it('shows a mismatch message when confirm differs', async () => {
    const w = await mountSuspended(PasswordRequirements, {
      props: { value: 'Abcdef1!', confirm: 'Abcdef2@' },
    })
    expect(w.text()).toContain('Passwords do not match.')
  })

  it('fails the personal-term rule when the value contains a forbidden term', async () => {
    const w = await mountSuspended(PasswordRequirements, {
      props: { value: 'Lovelace1!', forbidden: ['Lovelace'] },
    })
    expect(w.text()).not.toContain('Strong password')
  })
})
