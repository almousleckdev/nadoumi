import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PassportMismatchNotice from '~/components/onboarding/PassportMismatchNotice.vue'

const mismatches = [
  { field: 'givenName', passportValue: 'MARIE', profileValue: 'MARY' },
  { field: 'dob', passportValue: '2000-05-02', profileValue: '2000-05-01' },
] as const

describe('PassportMismatchNotice', () => {
  it('says the passport and profile are not the same and lists each field with both values', async () => {
    const w = await mountSuspended(PassportMismatchNotice, { props: { mismatches: [...mismatches] } })

    expect(w.text()).toContain('do not match')
    expect(w.text()).toContain('not the same as the details you entered')
    expect(w.text()).toContain('Given name')
    expect(w.text()).toContain('MARIE')
    expect(w.text()).toContain('MARY')
    expect(w.text()).toContain('Date of birth')
    expect(w.text()).toContain('2000-05-02')
  })

  it('lets the student jump back to the profile', async () => {
    const w = await mountSuspended(PassportMismatchNotice, { props: { mismatches: [...mismatches] } })

    await w.find('button').trigger('click')

    expect(w.emitted('edit-profile')).toHaveLength(1)
  })
})
