import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PasswordChangeForm from '~/components/dashboard/PasswordChangeForm.vue'

describe('PasswordChangeForm', () => {
  it('does not emit submit when the new password fails the policy', async () => {
    const w = await mountSuspended(PasswordChangeForm, { props: { busy: false } })
    await w.find('#pw-current').setValue('Whatever1!')
    await w.find('#pw-new').setValue('weak')
    await w.find('#pw-confirm').setValue('weak')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')).toBeFalsy()
    expect(w.text()).toContain('at least 8 characters')
  })

  it('does not emit submit when confirm does not match', async () => {
    const w = await mountSuspended(PasswordChangeForm, { props: { busy: false } })
    await w.find('#pw-current').setValue('OldPass1!')
    await w.find('#pw-new').setValue('NewPass1!')
    await w.find('#pw-confirm').setValue('Different1!')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')).toBeFalsy()
  })

  it('emits submit with current + next when valid', async () => {
    const w = await mountSuspended(PasswordChangeForm, { props: { busy: false } })
    await w.find('#pw-current').setValue('OldPass1!')
    await w.find('#pw-new').setValue('NewPass1!')
    await w.find('#pw-confirm').setValue('NewPass1!')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')?.[0]?.[0]).toEqual({ current: 'OldPass1!', next: 'NewPass1!' })
  })
})
