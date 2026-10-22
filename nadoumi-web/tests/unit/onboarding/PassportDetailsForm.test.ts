import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PassportDetailsForm from '~/components/onboarding/PassportDetailsForm.vue'

const initial = { passportNo: 'L898902C3', issueDate: '2024-01-01', expiryDate: '2099-01-01' }
const mountForm = (over: Record<string, unknown> = {}) =>
  mountSuspended(PassportDetailsForm, { props: { initial, busy: false, canSave: true, ...over } })

describe('PassportDetailsForm', () => {
  it('asks only for the number and the two dates — never the name or date of birth again', async () => {
    const w = await mountForm()

    expect((w.find('#passportNo').element as HTMLInputElement).value).toBe('L898902C3')
    expect((w.find('#passportExpiryDate').element as HTMLInputElement).value).toBe('2099-01-01')
    expect(w.findAll('input').map(i => i.attributes('id'))).toEqual(['passportNo', 'passportIssueDate', 'passportExpiryDate'])
  })

  it('emits the confirmed details when valid', async () => {
    const w = await mountForm()

    await w.find('form').trigger('submit')

    expect(w.emitted('submit')?.[0]?.[0]).toEqual(initial)
  })

  it('states the six-month rule and blocks a passport that expires too soon', async () => {
    const w = await mountForm()
    await w.find('#passportExpiryDate').setValue(new Date().toISOString().slice(0, 10))
    await w.find('form').trigger('submit')

    expect(w.text()).toContain('more than six months')
    expect(w.emitted('submit')).toBeUndefined()
  })

  it('requires every field when nothing was saved yet', async () => {
    const w = await mountForm({ initial: null })

    await w.find('form').trigger('submit')

    expect(w.emitted('submit')).toBeUndefined()
    expect(w.findAll('[role="alert"]')).toHaveLength(3)
  })

  it('cannot be saved until a scan exists', async () => {
    const w = await mountForm({ canSave: false })

    expect(w.find('button[type="submit"]').attributes('disabled')).toBeDefined()
  })
})
